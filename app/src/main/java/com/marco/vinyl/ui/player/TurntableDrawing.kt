package com.marco.vinyl.ui.player

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.marco.vinyl.turntable.TurntableGeometry
import com.marco.vinyl.turntable.Vec2
import com.marco.vinyl.ui.theme.VinylColors
import kotlin.math.cos
import kotlin.math.sin

private const val HEADSHELL_ANGLE = 22f

internal fun Vec2.toOffset() = Offset(x, y)

internal fun DrawScope.drawPlinth(g: TurntableGeometry, motorOn: Boolean) {
    val w = g.width
    drawRoundRect(
        Brush.linearGradient(
            listOf(VinylColors.WoodLight, VinylColors.WoodMid, VinylColors.WoodDark),
            start = Offset.Zero,
            end = Offset(w, g.height),
        ),
        size = Size(w, g.height),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    // Maserung
    for (i in 0 until 14) {
        val y = g.height * (i + 0.5f) / 14f
        drawLine(
            Color.Black.copy(alpha = 0.06f),
            Offset(0f, y),
            Offset(w, y + w * 0.03f * sin(i * 1.7f)),
            strokeWidth = w * 0.004f,
        )
    }
    drawRoundRect(
        Color.White.copy(alpha = 0.08f),
        size = Size(w, g.height),
        cornerRadius = CornerRadius(w * 0.04f),
        style = Stroke(width = w * 0.006f),
    )

    // Start/Stopp-Knopf mit Kontrolllampe
    val knob = Offset(w * 0.86f, g.height * 0.9f)
    drawCircle(Color.Black.copy(alpha = 0.4f), w * 0.042f, knob + Offset(w * 0.004f, w * 0.008f))
    drawCircle(Brush.radialGradient(listOf(VinylColors.Steel, VinylColors.SteelDark), center = knob, radius = w * 0.04f), w * 0.038f, knob)
    val lamp = Offset(w * 0.76f, g.height * 0.9f)
    drawCircle(if (motorOn) Color(0xFFFF5A3C) else Color(0xFF4A1A12), w * 0.011f, lamp)
    if (motorOn) drawCircle(Color(0x55FF5A3C), w * 0.025f, lamp)
}

internal fun DrawScope.drawPlatter(g: TurntableGeometry, rotation: Float) {
    val c = g.platterCenter.toOffset()
    val r = g.platterRadius
    drawCircle(Color.Black.copy(alpha = 0.45f), r * 1.01f, c + Offset(r * 0.02f, r * 0.04f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF9EA3A8), Color(0xFF5D6166)), center = c, radius = r), r, c)
    // Stroboskop-Punkte am Rand drehen sich mit.
    rotate(rotation, c) {
        for (i in 0 until 90) {
            val a = Math.toRadians(i * 4.0)
            val p = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * 0.985f)
            drawCircle(Color(0xFF2B2E31), r * 0.008f, p)
        }
    }
    drawCircle(Color(0xFF1C1C1E), r * 0.965f, c)
    drawCircle(VinylColors.Steel, r * 0.03f, c)
}

/**
 * Tonarm mit Gegengewicht, Rohr und Tonkopf. Angehoben wirft er einen weiteren Schatten.
 */
internal fun DrawScope.drawTonearm(g: TurntableGeometry, angle: Float, lifted: Boolean) {
    val w = g.width
    val pivot = g.armPivot.toOffset()

    // Sockel und Armstütze
    drawCircle(Color.Black.copy(alpha = 0.35f), w * 0.075f, pivot + Offset(w * 0.006f, w * 0.01f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3D41), Color(0xFF1E2022)), center = pivot, radius = w * 0.075f), w * 0.072f, pivot)
    val rest = g.armPivot + (g.needleAt(g.restAngle) - g.armPivot) * 0.62f
    drawRoundRect(
        Color(0xFF2A2C2F),
        topLeft = rest.toOffset() + Offset(w * 0.012f, -w * 0.02f),
        size = Size(w * 0.03f, w * 0.04f),
        cornerRadius = CornerRadius(w * 0.006f),
    )

    val shadow = if (lifted) Offset(w * 0.028f, w * 0.036f) else Offset(w * 0.006f, w * 0.009f)
    translate(shadow.x, shadow.y) { armShape(g, angle, shadowOnly = true) }
    armShape(g, angle, shadowOnly = false)

    drawCircle(Brush.radialGradient(listOf(VinylColors.Steel, VinylColors.SteelDark), center = pivot, radius = w * 0.035f), w * 0.032f, pivot)
}

private fun DrawScope.armShape(g: TurntableGeometry, angle: Float, shadowOnly: Boolean) {
    val w = g.width
    val pivot = g.armPivot.toOffset()
    val shadow = Color.Black.copy(alpha = 0.32f)
    // Im lokalen Koordinatensystem zeigt der Arm nach unten (+y), [angle] = 90° entspricht 0° Drehung.
    rotate(angle - 90f, pivot) {
        // Der Tonkopf ist abgewinkelt; das Rohr endet so, dass die Nadelspitze genau bei
        // (0, armLength) liegt, also dort, wo TurntableGeometry.needleAt sie erwartet.
        val stylus = w * 0.069f
        val headAngle = Math.toRadians(HEADSHELL_ANGLE.toDouble())
        val tubeEnd = pivot + Offset(stylus * sin(headAngle).toFloat(), g.armLength - stylus * cos(headAngle).toFloat())
        val counterweight = pivot + Offset(0f, -w * 0.11f)

        drawRoundRect(
            if (shadowOnly) shadow else Color(0xFF2F3236),
            topLeft = counterweight - Offset(w * 0.034f, w * 0.03f),
            size = Size(w * 0.068f, w * 0.06f),
            cornerRadius = CornerRadius(w * 0.01f),
        )
        drawLine(
            if (shadowOnly) shadow else VinylColors.Steel,
            pivot - Offset(0f, w * 0.08f),
            tubeEnd,
            strokeWidth = w * 0.016f,
            cap = StrokeCap.Round,
        )
        if (!shadowOnly) {
            drawLine(Color.White.copy(alpha = 0.55f), pivot + Offset(-w * 0.003f, 0f), tubeEnd + Offset(-w * 0.003f, 0f), strokeWidth = w * 0.004f)
        }
        // Tonkopf, leicht abgewinkelt
        rotate(HEADSHELL_ANGLE, tubeEnd) {
            drawRoundRect(
                if (shadowOnly) shadow else Color(0xFF1F2124),
                topLeft = tubeEnd - Offset(w * 0.024f, w * 0.004f),
                size = Size(w * 0.048f, w * 0.085f),
                cornerRadius = CornerRadius(w * 0.008f),
            )
            if (!shadowOnly) {
                drawLine(VinylColors.Steel, tubeEnd + Offset(w * 0.02f, w * 0.015f), tubeEnd + Offset(w * 0.05f, w * 0.01f), strokeWidth = w * 0.006f, cap = StrokeCap.Round)
                drawRect(VinylColors.Brass, topLeft = tubeEnd + Offset(-w * 0.012f, stylus - w * 0.018f), size = Size(w * 0.024f, w * 0.018f))
            }
        }
    }
}
