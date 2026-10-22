package com.marco.vinyl.turntable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurntableGeometryTest {

    private val g = TurntableGeometry(width = 1000f)

    @Test
    fun restingNeedleIsOutsideTheRecord() {
        assertTrue(g.needleRadius(g.restAngle) > g.recordRadius)
        assertFalse(g.isOverGrooves(g.restAngle))
    }

    @Test
    fun grooveAnglesLieBetweenRestAndMax() {
        assertTrue(g.restAngle < g.outerGrooveAngle)
        assertTrue(g.outerGrooveAngle < g.innerGrooveAngle)
        assertTrue(g.innerGrooveAngle < g.maxAngle)
        assertEquals(g.outerGrooveRadius, g.needleRadius(g.outerGrooveAngle), 0.5f)
        assertEquals(g.innerGrooveRadius, g.needleRadius(g.innerGrooveAngle), 0.5f)
    }

    @Test
    fun progressAndAngleRoundTrip() {
        for (p in listOf(0f, 0.1f, 0.33f, 0.5f, 0.8f, 1f)) {
            assertEquals(p, g.progressForAngle(g.angleForProgress(p)), 0.001f)
        }
    }

    @Test
    fun needleMovesInwardAsArmSwings() {
        var last = Float.MAX_VALUE
        var a = g.restAngle
        while (a <= g.maxAngle) {
            val r = g.needleRadius(a)
            assertTrue("radius must shrink at $a°", r < last)
            last = r
            a += 0.5f
        }
    }

    @Test
    fun clampKeepsArmInRange() {
        assertEquals(g.restAngle, g.clampAngle(10f), 0f)
        assertEquals(g.maxAngle, g.clampAngle(179f), 0f)
    }

    @Test
    fun pointOnArmIsCloseToArm() {
        val angle = g.angleForProgress(0.4f)
        val mid = g.armPivot + (g.needleAt(angle) - g.armPivot) * 0.5f
        assertEquals(0f, g.distanceToArm(mid, angle), 0.01f)
        assertTrue(g.distanceToArm(g.platterCenter, g.restAngle) > g.width * 0.07f)
    }

    @Test
    fun angleTowardsNeedleMatchesArmAngle() {
        val angle = 117f
        assertEquals(angle, g.angleTowards(g.needleAt(angle)), 0.01f)
    }
}
