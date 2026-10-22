package com.marco.vinyl.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marco.vinyl.model.Album
import com.marco.vinyl.ui.components.AlbumCover
import com.marco.vinyl.ui.components.VinylRecord
import com.marco.vinyl.ui.theme.VinylColors
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class DragAxis { None, Horizontal, Vertical }

/**
 * Die aus dem Schrank geholte Platte: links die Hülle, dahinter die Platte.
 * 1. Nach rechts ziehen → die Platte rutscht aus der Hülle.
 * 2. Nach oben wischen → sie fliegt aus dem Bild und liegt danach vorne neben dem Plattenspieler.
 */
@Composable
fun SleeveDetail(
    album: Album,
    onDismiss: () -> Unit,
    onSendToFront: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember(album.id) { Animatable(0f) }
    LaunchedEffect(album.id) { appear.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 300f)) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * appear.value))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * size.height * 0.3f
                    scaleX = 0.85f + 0.15f * appear.value
                    scaleY = scaleX
                },
        ) {
            val sleeve = minOf(maxWidth / 2.2f, maxHeight * 0.55f)
            val stageHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SleeveStage(album, sleeve.value, stageHeightPx, onSendToFront)
                Spacer(Modifier.height(20.dp))
                TrackList(album, Modifier.width(sleeve * 2.1f))
            }
        }
    }
}

@Composable
private fun SleeveStage(album: Album, sleeveDp: Float, flyAwayPx: Float, onSendToFront: (Album) -> Unit) {
    val density = LocalDensity.current
    val sleevePx = with(density) { sleeveDp.dp.toPx() }
    val peekX = sleevePx * 0.10f
    val outX = sleevePx * 1.08f

    val recordX = remember(album.id) { Animatable(peekX) }
    val recordY = remember(album.id) { Animatable(0f) }
    var isOut by remember(album.id) { mutableStateOf(false) }
    var sent by remember(album.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val send by rememberUpdatedState(onSendToFront)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width((sleeveDp * 2.1f).dp)
                .height(sleeveDp.dp)
                // Klicks auf die Bühne sollen das Overlay nicht schließen.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .pointerInput(album.id) {
                    var axis = DragAxis.None
                    val velocity = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            axis = DragAxis.None
                            velocity.resetTracking()
                        },
                        onDrag = { change, drag ->
                            if (sent) return@detectDragGestures
                            change.consume()
                            velocity.addPosition(change.uptimeMillis, change.position)
                            if (axis == DragAxis.None) {
                                axis = if (isOut && abs(drag.y) > abs(drag.x)) DragAxis.Vertical else DragAxis.Horizontal
                            }
                            scope.launch {
                                when (axis) {
                                    DragAxis.Horizontal -> recordX.snapTo((recordX.value + drag.x).coerceIn(peekX, outX))
                                    DragAxis.Vertical -> recordY.snapTo((recordY.value + drag.y).coerceAtMost(sleevePx * 0.1f))
                                    DragAxis.None -> Unit
                                }
                            }
                        },
                        onDragEnd = {
                            val v = velocity.calculateVelocity()
                            scope.launch {
                                when (axis) {
                                    DragAxis.Horizontal -> {
                                        isOut = recordX.value > outX * 0.5f || v.x > 1200f
                                        recordX.animateTo(if (isOut) outX else peekX, spring(dampingRatio = 0.7f))
                                    }
                                    DragAxis.Vertical -> {
                                        if (recordY.value < -sleevePx * 0.3f || v.y < -1500f) {
                                            sent = true
                                            recordY.animateTo(-(flyAwayPx + sleevePx * 2f), tween(380))
                                            send(album)
                                        } else {
                                            recordY.animateTo(0f, spring(dampingRatio = 0.6f))
                                        }
                                    }
                                    DragAxis.None -> Unit
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                recordX.animateTo(if (isOut) outX else peekX)
                                recordY.animateTo(0f)
                            }
                        },
                    )
                },
        ) {
            // Platte zuerst zeichnen, damit sie hinter der Hülle steckt.
            VinylRecord(
                album = album,
                rotation = (recordX.value - peekX) / sleevePx * 120f,
                modifier = Modifier
                    .size((sleeveDp * 0.96f).dp)
                    .graphicsLayer {
                        translationX = sleevePx * 0.02f + recordX.value
                        translationY = sleevePx * 0.02f + recordY.value
                    },
            )
            AlbumCover(
                album = album,
                modifier = Modifier
                    .size(sleeveDp.dp)
                    .shadow(16.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                sent -> "Unterwegs nach vorne …"
                isOut -> "↑  Nach oben wischen, um die Platte nach vorne zu schicken"
                else -> "Platte nach rechts aus der Hülle ziehen  →"
            },
            style = MaterialTheme.typography.titleMedium,
            color = VinylColors.Brass,
        )
    }
}

@Composable
private fun TrackList(album: Album, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            "${album.artist} · ${album.title}" + (album.year?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(album.tracks) { i, track ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        "A${i + 1}",
                        Modifier.width(40.dp),
                        color = VinylColors.Brass,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        track.title,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(formatDuration(track.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
