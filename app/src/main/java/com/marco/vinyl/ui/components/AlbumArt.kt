package com.marco.vinyl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.marco.vinyl.model.Album
import com.marco.vinyl.model.SleevePattern
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Vorderseite der Plattenhülle. */
@Composable
fun AlbumCover(album: Album, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val primary = Color(album.sleeve.primary)
        val secondary = Color(album.sleeve.secondary)
        drawRect(primary)
        clipRect { drawPattern(album.sleeve.pattern, secondary) }

        // Leichte Abnutzung am Rand, wie bei einer oft gespielten Hülle.
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                center = center,
                radius = size.minDimension * 0.75f,
            ),
        )

        val pad = size.width * 0.07f
        val maxWidth = (size.width - 2 * pad).toInt().coerceAtLeast(1)
        val title = textMeasurer.measure(
            album.title,
            TextStyle(color = secondary, fontSize = (size.width * 0.085f).toSp(), fontWeight = FontWeight.Black),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxWidth),
        )
        val artist = textMeasurer.measure(
            album.artist.uppercase() + (album.year?.let { "  ·  $it" } ?: ""),
            TextStyle(color = secondary.copy(alpha = 0.85f), fontSize = (size.width * 0.04f).toSp(), fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxWidth),
        )
        drawText(artist, topLeft = Offset(pad, size.height - pad - artist.size.height))
        drawText(title, topLeft = Offset(pad, size.height - pad - artist.size.height - title.size.height - size.height * 0.01f))
    }
}

/** Plattenrücken, wie er im Schrank zu sehen ist. */
@Composable
fun AlbumSpine(album: Album, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val primary = Color(album.sleeve.primary)
        val secondary = Color(album.sleeve.secondary)
        val corner = CornerRadius(size.width * 0.12f)
        drawRoundRect(primary, cornerRadius = corner)
        drawRect(secondary, topLeft = Offset(0f, size.height * 0.06f), size = Size(size.width, size.height * 0.012f))
        drawRect(secondary, topLeft = Offset(0f, size.height * 0.93f), size = Size(size.width, size.height * 0.012f))
        // Kanten: links Licht, rechts Schatten.
        drawRect(Color.White.copy(alpha = 0.12f), size = Size(size.width * 0.12f, size.height))
        drawRect(
            Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(size.width * 0.86f, 0f),
            size = Size(size.width * 0.14f, size.height),
        )

        val label = textMeasurer.measure(
            "${album.artist.uppercase()}  —  ${album.title}",
            TextStyle(color = secondary, fontSize = (size.width * 0.36f).toSp(), fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = (size.height * 0.8f).toInt().coerceAtLeast(1)),
        )
        rotate(-90f, center) {
            drawText(label, topLeft = center - Offset(label.size.width / 2f, label.size.height / 2f))
        }

        if (dimmed) drawRoundRect(Color.Black.copy(alpha = 0.55f), cornerRadius = corner)
    }
}

private fun DrawScope.drawPattern(pattern: SleevePattern, color: Color) {
    val w = size.width
    val h = size.height
    when (pattern) {
        SleevePattern.Circles -> {
            for (i in 6 downTo 1) {
                drawCircle(
                    color.copy(alpha = 0.12f + 0.1f * (6 - i)),
                    radius = w * 0.08f * i,
                    center = Offset(w * 0.62f, h * 0.38f),
                    style = Stroke(width = w * 0.018f),
                )
            }
            drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.62f, h * 0.38f))
        }
        SleevePattern.Stripes -> {
            for (i in 0 until 9) {
                val y = h * (0.06f + i * 0.065f)
                drawRect(color.copy(alpha = 0.25f + 0.08f * i), topLeft = Offset(0f, y), size = Size(w, h * 0.03f))
            }
        }
        SleevePattern.Grid -> {
            val n = 7
            val cell = w / n
            for (x in 0 until n) for (y in 0 until 4) {
                if ((x * 3 + y * 5) % 4 == 0) continue
                drawRoundRect(
                    color.copy(alpha = 0.18f + 0.12f * ((x + y) % 4)),
                    topLeft = Offset(x * cell + cell * 0.12f, y * cell + cell * 0.12f),
                    size = Size(cell * 0.76f, cell * 0.76f),
                    cornerRadius = CornerRadius(cell * 0.12f),
                )
            }
        }
        SleevePattern.Sunburst -> {
            val origin = Offset(w * 0.5f, h * 0.55f)
            val rays = 18
            for (i in 0 until rays step 2) {
                val a0 = PI * i / rays
                val a1 = PI * (i + 1) / rays
                val path = Path().apply {
                    moveTo(origin.x, origin.y)
                    lineTo(origin.x + cos(a0).toFloat() * w * 1.2f, origin.y - sin(a0).toFloat() * w * 1.2f)
                    lineTo(origin.x + cos(a1).toFloat() * w * 1.2f, origin.y - sin(a1).toFloat() * w * 1.2f)
                    close()
                }
                drawPath(path, color.copy(alpha = 0.35f))
            }
            drawCircle(color, radius = w * 0.16f, center = origin)
        }
        SleevePattern.Waves -> {
            for (row in 0 until 8) {
                val path = Path()
                val baseY = h * (0.08f + row * 0.07f)
                path.moveTo(0f, baseY)
                var x = 0f
                while (x <= w) {
                    path.lineTo(x, baseY + sin(x / w * 4 * PI + row).toFloat() * h * 0.02f)
                    x += w / 60f
                }
                drawPath(path, color.copy(alpha = 0.3f + 0.08f * row), style = Stroke(width = w * 0.012f))
            }
        }
    }
}
