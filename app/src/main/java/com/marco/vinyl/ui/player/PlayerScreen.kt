package com.marco.vinyl.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.marco.vinyl.model.Album
import com.marco.vinyl.model.Track
import com.marco.vinyl.turntable.GrooveMap
import com.marco.vinyl.turntable.TurntableGeometry
import com.marco.vinyl.turntable.Vec2
import com.marco.vinyl.ui.components.VinylRecord
import com.marco.vinyl.ui.components.drawVinyl
import com.marco.vinyl.ui.library.formatDuration
import com.marco.vinyl.ui.theme.VinylColors
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val RPM_33_DEG_PER_SEC = 200f

private enum class DeckDrag { None, Arm, Record }

/**
 * Außendisplay: der Plattenspieler.
 * - Wartende Platte unten auf den Plattenteller ziehen (oder antippen).
 * - Tonarm über die Platte schwenken und loslassen → Nadel setzt auf, die Stelle bestimmt den Titel.
 * - Tonarm zurück auf die Stütze → Stopp. Bei stehendem Arm lässt sich die Platte vom Teller ziehen.
 */
@Composable
fun PlayerScreen(
    platterRecord: Album?,
    pendingRecord: Album?,
    needleProgress: Float?,
    currentTrack: Track?,
    currentTrackIndex: Int,
    positionMs: Long,
    error: String?,
    onPlacePending: () -> Unit,
    onRemoveRecord: () -> Unit,
    onDropNeedle: (Float) -> Unit,
    onLiftNeedle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(VinylColors.Background)
            .safeDrawingPadding(),
    ) {
        val density = LocalDensity.current
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val portrait = maxH >= maxW * 1.3f
        val deckWidth = if (portrait) {
            min(maxW, maxH * 0.62f / TurntableGeometry.HEIGHT_RATIO)
        } else {
            min(maxW * 0.64f, maxH / TurntableGeometry.HEIGHT_RATIO)
        }
        val g = remember(deckWidth) { TurntableGeometry(deckWidth) }
        val deckOrigin = if (portrait) Offset((maxW - deckWidth) / 2f, 0f) else Offset(0f, (maxH - g.height) / 2f)
        val platterOnScreen = deckOrigin + Offset(g.platterCenter.x, g.platterCenter.y)
        val trayCenter = if (portrait) {
            Offset(maxW / 2f, maxH - g.recordRadius * 0.75f)
        } else {
            Offset(maxW - g.recordRadius * 0.6f, maxH - g.recordRadius * 0.7f)
        }

        Deck(
            g = g,
            platterRecord = platterRecord,
            needleProgress = needleProgress,
            onRemoveRecord = onRemoveRecord,
            onDropNeedle = onDropNeedle,
            onLiftNeedle = onLiftNeedle,
            modifier = Modifier.offset { IntOffset(deckOrigin.x.roundToInt(), deckOrigin.y.roundToInt()) },
        )

        val infoModifier = if (portrait) {
            Modifier
                .offset { IntOffset(0, (deckOrigin.y + g.height).roundToInt()) }
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.TopCenter)
        } else {
            Modifier
                .align(Alignment.TopEnd)
                .width(with(density) { (maxW - deckWidth).toDp() })
                .padding(16.dp)
        }
        NowPlayingInfo(
            platterRecord = platterRecord,
            pendingRecord = pendingRecord,
            needleDown = needleProgress != null,
            currentTrack = currentTrack,
            currentTrackIndex = currentTrackIndex,
            positionMs = positionMs,
            error = error,
            modifier = infoModifier,
        )

        if (pendingRecord != null) {
            PendingRecord(
                album = pendingRecord,
                radius = g.recordRadius,
                home = trayCenter,
                target = platterOnScreen,
                acceptRadius = g.platterRadius * 0.75f,
                onPlaced = onPlacePending,
            )
        }
    }
}

@Composable
private fun Deck(
    g: TurntableGeometry,
    platterRecord: Album?,
    needleProgress: Float?,
    onRemoveRecord: () -> Unit,
    onDropNeedle: (Float) -> Unit,
    onLiftNeedle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()

    var armDragAngle by remember { mutableStateOf<Float?>(null) }
    // Hält den Arm nach dem Loslassen an der Absetzstelle, bis der Zustand „Nadel unten“ angekommen ist.
    var armHold by remember { mutableStateOf<Float?>(null) }
    val needleDown = needleProgress != null
    LaunchedEffect(needleDown) { if (needleDown) armHold = null }

    val armTarget = armDragAngle ?: armHold ?: needleProgress?.let { g.angleForProgress(it) } ?: g.restAngle
    val armAngle by animateFloatAsState(
        targetValue = armTarget,
        animationSpec = if (armDragAngle != null) snap() else spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "tonearm",
    )

    val armOffRest = (armDragAngle ?: armHold ?: if (needleDown) g.outerGrooveAngle else g.restAngle) > g.restAngle + 2f
    val spinning = platterRecord != null && (needleDown || armOffRest)
    val (rotation, speed) = rememberPlatterRotation(spinning)

    val recordOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val boundaries = remember(platterRecord) { platterRecord?.let { GrooveMap.of(it).boundaries } ?: emptyList() }

    val current by rememberUpdatedState(DeckInput(platterRecord, needleDown, armAngle))
    val drop by rememberUpdatedState(onDropNeedle)
    val lift by rememberUpdatedState(onLiftNeedle)
    val remove by rememberUpdatedState(onRemoveRecord)

    Canvas(
        modifier
            .size(with(density) { g.width.toDp() }, with(density) { g.height.toDp() })
            .semantics { contentDescription = "Plattenspieler" }
            .pointerInputDeck(
                g = g,
                current = { current },
                onArm = { armDragAngle = it },
                onArmGrabbed = { if (current.needleDown) lift() },
                onArmReleased = { angle ->
                    armDragAngle = null
                    if (current.record != null && g.isOverGrooves(angle)) {
                        armHold = angle
                        drop(g.progressForAngle(angle))
                    } else {
                        armHold = null
                    }
                },
                onRecordDrag = { delta -> scope.launch { recordOffset.snapTo(recordOffset.value + delta) } },
                onRecordReleased = {
                    scope.launch {
                        if (recordOffset.value.getDistance() > g.recordRadius * 0.8f) {
                            remove()
                            recordOffset.snapTo(Offset.Zero)
                        } else {
                            recordOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f))
                        }
                    }
                },
            ),
    ) {
        drawPlinth(g, motorOn = spinning || speed > 0f)
        drawPlatter(g, rotation)
        if (platterRecord != null) {
            drawVinyl(
                album = platterRecord,
                center = g.platterCenter.toOffset() + recordOffset.value,
                radius = g.recordRadius,
                rotation = rotation,
                textMeasurer = textMeasurer,
                trackBoundaries = boundaries,
                shadow = recordOffset.value != Offset.Zero,
            )
        }
        drawTonearm(g, armAngle, lifted = !needleDown || armDragAngle != null)
    }
}

private data class DeckInput(val record: Album?, val needleDown: Boolean, val armAngle: Float)

private fun Modifier.pointerInputDeck(
    g: TurntableGeometry,
    current: () -> DeckInput,
    onArm: (Float) -> Unit,
    onArmGrabbed: () -> Unit,
    onArmReleased: (Float) -> Unit,
    onRecordDrag: (Offset) -> Unit,
    onRecordReleased: () -> Unit,
): Modifier = pointerInput(g) {
    var target = DeckDrag.None
    var grabOffset = 0f
    var angle = g.restAngle
    detectDragGestures(
        onDragStart = { start ->
            val state = current()
            val p = Vec2(start.x, start.y)
            target = when {
                g.distanceToArm(p, state.armAngle) < g.width * 0.07f -> DeckDrag.Arm
                state.record != null && !state.needleDown && state.armAngle < g.restAngle + 2f &&
                    (p - g.platterCenter).length() < g.recordRadius -> DeckDrag.Record
                else -> DeckDrag.None
            }
            if (target == DeckDrag.Arm) {
                angle = state.armAngle
                grabOffset = state.armAngle - g.angleTowards(p)
                onArmGrabbed()
                onArm(angle)
            }
        },
        onDrag = { change, drag ->
            when (target) {
                DeckDrag.Arm -> {
                    change.consume()
                    angle = g.clampAngle(g.angleTowards(Vec2(change.position.x, change.position.y)) + grabOffset)
                    onArm(angle)
                }
                DeckDrag.Record -> {
                    change.consume()
                    onRecordDrag(drag)
                }
                DeckDrag.None -> Unit
            }
        },
        onDragEnd = {
            when (target) {
                DeckDrag.Arm -> onArmReleased(angle)
                DeckDrag.Record -> onRecordReleased()
                DeckDrag.None -> Unit
            }
            target = DeckDrag.None
        },
        onDragCancel = {
            when (target) {
                DeckDrag.Arm -> onArmReleased(g.restAngle)
                DeckDrag.Record -> onRecordReleased()
                DeckDrag.None -> Unit
            }
            target = DeckDrag.None
        },
    )
}

/** Drehwinkel des Tellers mit Anlauf und Auslauf wie bei einem echten Motor. */
@Composable
private fun rememberPlatterRotation(spinning: Boolean): Pair<Float, Float> {
    var rotation by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(spinning) {
        val target = if (spinning) RPM_33_DEG_PER_SEC else 0f
        val accel = if (spinning) 260f else 140f
        var last = withFrameNanos { it }
        while (spinning || speed > 0f) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            speed = if (speed < target) min(target, speed + accel * dt) else max(target, speed - accel * dt)
            rotation = (rotation + speed * dt) % 360f
        }
    }
    return rotation to speed
}

/** Die nach vorne geschickte Platte, bereit zum Auflegen. */
@Composable
private fun PendingRecord(
    album: Album,
    radius: Float,
    home: Offset,
    target: Offset,
    acceptRadius: Float,
    onPlaced: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val position = remember(album.id) { Animatable(home, Offset.VectorConverter) }
    // Landet von oben, als käme sie gerade vom Innendisplay.
    LaunchedEffect(album.id) {
        position.snapTo(home - Offset(0f, radius * 3f))
        position.animateTo(home, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow))
    }
    val placed by rememberUpdatedState(onPlaced)
    val homeNow by rememberUpdatedState(home)
    val targetNow by rememberUpdatedState(target)

    fun settle() {
        scope.launch {
            if ((position.value - targetNow).getDistance() < acceptRadius) {
                position.animateTo(targetNow, spring(dampingRatio = 0.8f))
                placed()
            } else {
                position.animateTo(homeNow, spring(dampingRatio = 0.65f))
            }
        }
    }

    VinylRecord(
        album = album,
        modifier = Modifier
            .offset { IntOffset((position.value.x - radius).roundToInt(), (position.value.y - radius).roundToInt()) }
            .size(with(density) { (radius * 2).toDp() })
            .graphicsLayer {
                val lift = 1f + 0.04f * ((position.value - home).getDistance() / radius).coerceIn(0f, 1f)
                scaleX = lift
                scaleY = lift
            }
            .semantics { contentDescription = "${album.title}: auf den Plattenteller ziehen" }
            .pointerInput(album.id) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch { position.snapTo(position.value + drag) }
                    },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() },
                )
            }
            .pointerInput(album.id, "tap") {
                detectTapGestures {
                    scope.launch {
                        position.animateTo(targetNow, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
                        placed()
                    }
                }
            },
    )
}

@Composable
private fun NowPlayingInfo(
    platterRecord: Album?,
    pendingRecord: Album?,
    needleDown: Boolean,
    currentTrack: Track?,
    currentTrackIndex: Int,
    positionMs: Long,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val (headline, detail) = when {
            platterRecord != null && needleDown && currentTrack != null ->
                "A${currentTrackIndex + 1}  ${currentTrack.title}" to
                    "${platterRecord.artist} · ${platterRecord.title}   ${formatDuration(positionMs)} / ${formatDuration(currentTrack.durationMs)}"
            pendingRecord != null && platterRecord == null ->
                "„${pendingRecord.title}“ ist da" to "Zieh die Platte auf den Plattenteller."
            pendingRecord != null ->
                "„${pendingRecord.title}“ wartet" to "Zieh sie auf den Teller, um „${platterRecord?.title}“ zu tauschen."
            platterRecord != null ->
                platterRecord.title to "Schwenk den Tonarm über die Platte und lass ihn los. Wo die Nadel aufsetzt, bestimmt den Titel."
            else ->
                "Keine Platte aufgelegt" to "Klapp das Fold auf und hol dir eine Platte aus dem Schrank."
        }
        Text(
            headline,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
