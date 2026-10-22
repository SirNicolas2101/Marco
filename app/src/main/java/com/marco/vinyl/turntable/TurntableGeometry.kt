package com.marco.vinyl.turntable

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(f: Float) = Vec2(x * f, y * f)
    fun length(): Float = hypot(x, y)
    fun dot(o: Vec2): Float = x * o.x + y * o.y
}

/** Proportionen einer 12"-Platte, relativ zum Plattenradius. Gilt für Schrank und Plattenspieler. */
object VinylSpec {
    const val OUTER_GROOVE = 0.945f
    const val INNER_GROOVE = 0.425f
    const val LABEL = 0.33f
    const val SPINDLE = 0.028f

    fun grooveRadius(recordRadius: Float, progress: Float): Float =
        recordRadius * (OUTER_GROOVE - progress.coerceIn(0f, 1f) * (OUTER_GROOVE - INNER_GROOVE))
}

/**
 * Maße des Plattenspielers in Pixeln, abgeleitet aus der Breite [width].
 * Winkel in Grad, gemessen wie `atan2` in Bildschirmkoordinaten (y nach unten):
 * 90° heißt, der Tonarm zeigt senkrecht nach unten, das ist die Ruheposition.
 */
class TurntableGeometry(val width: Float) {
    val height: Float = width * HEIGHT_RATIO

    val platterCenter = Vec2(0.42f * width, 0.50f * width)
    val platterRadius = 0.38f * width
    val recordRadius = 0.365f * width
    val outerGrooveRadius = recordRadius * VinylSpec.OUTER_GROOVE
    val innerGrooveRadius = recordRadius * VinylSpec.INNER_GROOVE

    val armPivot = Vec2(0.86f * width, 0.12f * width)
    val armLength = 0.62f * width
    val restAngle = 90f

    /** Ab diesem Winkel liegt die Nadel über der Einlaufrille. */
    val outerGrooveAngle: Float = angleForRadius(outerGrooveRadius)

    /** Auslaufrille, weiter nach innen lässt sich der Arm nur minimal schwenken. */
    val innerGrooveAngle: Float = angleForRadius(innerGrooveRadius)
    val maxAngle: Float = innerGrooveAngle + 1.5f

    fun needleAt(angleDeg: Float): Vec2 {
        val rad = Math.toRadians(angleDeg.toDouble())
        return armPivot + Vec2(cos(rad).toFloat(), sin(rad).toFloat()) * armLength
    }

    fun needleRadius(angleDeg: Float): Float = (needleAt(angleDeg) - platterCenter).length()

    fun clampAngle(angleDeg: Float): Float = angleDeg.coerceIn(restAngle, maxAngle)

    /** Richtung vom Drehpunkt des Tonarms zu [point], in Grad. */
    fun angleTowards(point: Vec2): Float {
        val d = point - armPivot
        return Math.toDegrees(atan2(d.y, d.x).toDouble()).toFloat()
    }

    fun isOverGrooves(angleDeg: Float): Boolean = angleDeg >= outerGrooveAngle

    fun progressForAngle(angleDeg: Float): Float =
        ((outerGrooveRadius - needleRadius(angleDeg)) / (outerGrooveRadius - innerGrooveRadius)).coerceIn(0f, 1f)

    fun angleForProgress(progress: Float): Float =
        angleForRadius(VinylSpec.grooveRadius(recordRadius, progress))

    /** Kürzester Abstand von [point] zum Tonarm (Drehpunkt bis Nadel) bei [angleDeg]. */
    fun distanceToArm(point: Vec2, angleDeg: Float): Float {
        val a = armPivot
        val b = needleAt(angleDeg)
        val ab = b - a
        val t = ((point - a).dot(ab) / ab.dot(ab)).coerceIn(0f, 1f)
        return (point - (a + ab * t)).length()
    }

    /**
     * Zwischen Ruheposition und dem Punkt, an dem der Arm über die Plattenmitte zeigt,
     * nimmt der Abstand der Nadel zur Mitte streng ab, daher reicht eine Bisektion.
     */
    private fun angleForRadius(radius: Float): Float {
        var lo = restAngle
        var hi = angleTowards(platterCenter)
        repeat(40) {
            val mid = (lo + hi) / 2f
            if (needleRadius(mid) > radius) lo = mid else hi = mid
        }
        return (lo + hi) / 2f
    }

    companion object {
        const val HEIGHT_RATIO = 0.96f
    }
}
