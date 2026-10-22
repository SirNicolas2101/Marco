package com.marco.vinyl.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marco.vinyl.model.Album
import com.marco.vinyl.model.Track
import com.marco.vinyl.ui.components.VinylRecord
import com.marco.vinyl.ui.theme.VinylColors

/** Innendisplay: der Plattenschrank. */
@Composable
fun LibraryScreen(
    albums: List<Album>,
    loading: Boolean,
    error: String?,
    sourceName: String,
    selected: Album?,
    pendingRecord: Album?,
    platterRecord: Album?,
    playingTrack: Track?,
    onSelect: (Album) -> Unit,
    onDismissSelection: () -> Unit,
    onSendToFront: (Album) -> Unit,
    onLiftNeedle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(VinylColors.Background),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(sourceName, albums.size)
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (albums.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(error ?: "Der Schrank ist leer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                RecordShelf(
                    albums = albums,
                    selectedId = selected?.id,
                    awayIds = setOfNotNull(pendingRecord?.id, platterRecord?.id),
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                )
            }
        }

        StatusCard(
            pendingRecord = pendingRecord,
            platterRecord = platterRecord,
            playingTrack = playingTrack,
            onLiftNeedle = onLiftNeedle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp),
        )

        if (selected != null) {
            SleeveDetail(album = selected, onDismiss = onDismissSelection, onSendToFront = onSendToFront)
        }
    }
}

@Composable
private fun Header(sourceName: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            "Plattenschrank",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$count Platten · Quelle: $sourceName",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Zeigt, was gerade vorne liegt oder läuft. */
@Composable
private fun StatusCard(
    pendingRecord: Album?,
    platterRecord: Album?,
    playingTrack: Track?,
    onLiftNeedle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = pendingRecord != null || platterRecord != null
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it * 2 },
        exit = slideOutVertically { it * 2 },
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val shown = pendingRecord ?: platterRecord
                if (shown != null) VinylRecord(shown, Modifier.size(48.dp))
                Column(Modifier.widthIn(max = 380.dp)) {
                    val (title, subtitle) = when {
                        pendingRecord != null ->
                            "„${pendingRecord.title}“ liegt vorne bereit" to
                                "Klapp das Fold zu und leg die Platte auf den Plattenteller."
                        playingTrack != null && platterRecord != null ->
                            "Läuft: ${playingTrack.title}" to "${platterRecord.artist} · ${platterRecord.title}"
                        platterRecord != null ->
                            "„${platterRecord.title}“ liegt auf dem Plattenteller" to
                                "Zuklappen und die Nadel auflegen, um abzuspielen."
                        else -> "" to ""
                    }
                    Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (playingTrack != null && pendingRecord == null) {
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(onClick = onLiftNeedle) { Text("Nadel heben") }
                }
            }
        }
    }
}
