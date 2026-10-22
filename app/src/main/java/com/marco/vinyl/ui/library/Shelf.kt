package com.marco.vinyl.ui.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marco.vinyl.model.Album
import com.marco.vinyl.ui.components.AlbumSpine
import com.marco.vinyl.ui.theme.VinylColors

private val SpineWidth = 38.dp
private val SpineHeight = 250.dp
private val SpineGap = 3.dp

/**
 * Der Plattenschrank: Fächer untereinander (vertikal scrollbar), in jedem Fach stehen die Platten
 * mit dem Rücken nach vorne. Antippen zieht die Platte ein Stück heraus und öffnet sie.
 */
@Composable
fun RecordShelf(
    albums: List<Album>,
    selectedId: String?,
    awayIds: Set<String>,
    onSelect: (Album) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    BoxWithConstraints(modifier) {
        val usable = maxWidth - 48.dp
        val perShelf = ((usable + SpineGap) / (SpineWidth + SpineGap)).toInt().coerceAtLeast(1)
        val shelves = albums.chunked(perShelf).let { if (it.size < 3) it + List(3 - it.size) { emptyList() } else it }

        LazyColumn(contentPadding = contentPadding) {
            items(shelves.size) { index ->
                ShelfCompartment(shelves[index], selectedId, awayIds, onSelect)
            }
        }
    }
}

@Composable
private fun ShelfCompartment(
    albums: List<Album>,
    selectedId: String?,
    awayIds: Set<String>,
    onSelect: (Album) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(SpineHeight + 36.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1A110B), VinylColors.WoodDark, Color(0xFF24170E))),
                ),
        ) {
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(SpineGap),
                verticalAlignment = Alignment.Bottom,
            ) {
                albums.forEachIndexed { i, album ->
                    val lifted by animateDpAsState(
                        targetValue = if (album.id == selectedId) (-28).dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "spine-lift",
                    )
                    // Leicht unterschiedliche Höhen lassen das Fach lebendiger wirken.
                    val height = SpineHeight - (((i * 7) % 4) * 3).dp
                    AlbumSpine(
                        album = album,
                        dimmed = album.id in awayIds,
                        modifier = Modifier
                            .offset(y = lifted)
                            .width(SpineWidth)
                            .height(height)
                            .semantics { contentDescription = "${album.title} von ${album.artist}" }
                            .clickable { onSelect(album) },
                    )
                }
            }
        }
        // Fachboden
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(Brush.verticalGradient(listOf(VinylColors.WoodLight, VinylColors.WoodMid, Color(0xFF2A1A10)))),
        )
    }
}
