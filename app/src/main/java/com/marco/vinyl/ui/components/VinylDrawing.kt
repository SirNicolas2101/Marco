package com.marco.vinyl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.marco.vinyl.model.Album
import com.marco.vinyl.turntable.GrooveMap
import com.marco.vinyl.turntable.VinylSpec
import com.marco.vinyl.ui.theme.VinylColors

/** Eine Platte als eigenständiges Element, z. B. beim Herausziehen aus der Hülle. */
@Composable
fun VinylRecord(album: Album, modifier: Modifier = Modifier, rotation: Float = 0f) {
    val textMeasurer = rememberTextMeasurer()
    val boundaries = remember(album) { GrooveMap.of(album).boundaries }
    Canvas(modifier) {
        drawVinyl(album, center, size.minDimension / 2f, rotation, textMeasurer, boundaries)
    }
}

/**
 * Zeichnet eine Platte mit Rillen, Lücken zwischen den Titeln, feststehendem Lichtreflex
 * und mitdrehendem Label.
 */
fun DrawScope.drawVinyl(
    album: Album,
    center: Offset,
    radius: Float,
    rotation: Float,
    textMeasurer: TextMeasurer,
    trackBoundaries: List<Float>,
    shadow: Boolean = true,
) {
    if (shadow) {
        drawCircle(Color.Black.copy(alpha = 0.45f), radius * 1.02f, center + Offset(radius * 0.02f, radius * 0.04f))
    }
    drawCircle(VinylColors.VinylBlack, radius, center)

    val outer = radius * VinylSpec.OUTER_GROOVE
    val inner = radius * VinylSpec.INNER_GROOVE
    val step = (radius * 0.012f).coerceAtLeast(1.6f)
    var r = inner
    var i = 0
    while (r < outer) {
        val alpha = if (i % 3 == 0) 0.07f else 0.035f
        drawCircle(Color.White.copy(alpha = alpha), r, center, style = Stroke(width = step * 0.35f))
        r += step
        i++
    }
    drawCircle(Color.White.copy(alpha = 0.10f), radius * 0.985f, center, style = Stroke(width = radius * 0.012f))

    // Glatte, glänzende Bänder zwischen den Titeln.
    for (b in trackBoundaries) {
        drawCircle(
            Color(0xFF2C2C2E),
            VinylSpec.grooveRadius(radius, b),
            center,
            style = Stroke(width = (radius * 0.012f).coerceAtLeast(2f)),
        )
    }

    // Der Reflex bleibt stehen, während sich die Platte darunter dreht.
    drawCircle(
        Brush.sweepGradient(
            0.00f to Color.Transparent,
            0.10f to Color.White.copy(alpha = 0.10f),
            0.18f to Color.Transparent,
            0.50f to Color.Transparent,
            0.60f to Color.White.copy(alpha = 0.08f),
            0.68f to Color.Transparent,
            1.00f to Color.Transparent,
            center = center,
        ),
        radius * 0.99f,
        center,
    )

    rotate(rotation, center) {
        drawLabel(album, center, radius * VinylSpec.LABEL, textMeasurer)
    }
    drawCircle(Color(0xFF0A0A0A), radius * VinylSpec.SPINDLE, center)
}

private fun DrawScope.drawLabel(album: Album, center: Offset, labelRadius: Float, textMeasurer: TextMeasurer) {
    val primary = Color(album.sleeve.secondary)
    val accent = Color(album.sleeve.primary)
    drawCircle(primary, labelRadius, center)
    drawCircle(accent, labelRadius * 0.93f, center, style = Stroke(width = labelRadius * 0.035f))
    drawCircle(accent.copy(alpha = 0.18f), labelRadius * 0.42f, center)

    val maxWidth = (labelRadius * 1.35f).toInt().coerceAtLeast(1)
    val title = textMeasurer.measure(
        album.title.uppercase(),
        TextStyle(color = accent, fontSize = (labelRadius * 0.17f).toSp(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        constraints = Constraints(maxWidth = maxWidth),
    )
    drawText(title, topLeft = center + Offset(-title.size.width / 2f, -labelRadius * 0.52f - title.size.height / 2f))

    val artist = textMeasurer.measure(
        album.artist,
        TextStyle(color = accent.copy(alpha = 0.85f), fontSize = (labelRadius * 0.13f).toSp(), textAlign = TextAlign.Center),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth),
    )
    drawText(artist, topLeft = center + Offset(-artist.size.width / 2f, labelRadius * 0.55f - artist.size.height / 2f))

    val side = textMeasurer.measure("A  ·  33⅓", TextStyle(color = accent.copy(alpha = 0.7f), fontSize = (labelRadius * 0.1f).toSp()))
    drawText(side, topLeft = center + Offset(labelRadius * 0.18f, -side.size.height / 2f))
}
