package com.marco.vinyl.turntable

import org.junit.Assert.assertEquals
import org.junit.Test

class GrooveMapTest {

    private val map = GrooveMap(listOf(60_000L, 30_000L, 90_000L)) // gesamt 180 s

    @Test
    fun boundariesFollowTrackLengths() {
        assertEquals(listOf(1f / 3f, 0.5f), map.boundaries)
    }

    @Test
    fun outerEdgeStartsFirstTrack() {
        assertEquals(GroovePosition(0, 0), map.locate(0f))
    }

    @Test
    fun dropInsideTrackKeepsPosition() {
        // 45 s in den ersten Titel
        assertEquals(GroovePosition(0, 45_000), map.locate(0.25f))
        // 120 s → 30 s in Titel 3
        assertEquals(GroovePosition(2, 30_000), map.locate(120f / 180f))
    }

    @Test
    fun dropInLeadInSnapsToTrackStart() {
        assertEquals(GroovePosition(1, 0), map.locate(61_000f / 180_000f))
    }

    @Test
    fun dropJustBeforeEndJumpsToNextTrack() {
        assertEquals(GroovePosition(1, 0), map.locate(59_500f / 180_000f))
    }

    @Test
    fun endOfRecordStaysOnLastTrack() {
        val pos = map.locate(1f)
        assertEquals(2, pos.trackIndex)
        assertEquals(89_999L, pos.positionMs)
    }

    @Test
    fun progressRoundTrip() {
        assertEquals(0.5f + 15_000f / 180_000f, map.progressOf(2, 15_000), 1e-6f)
        val p = map.progressOf(1, 10_000)
        assertEquals(GroovePosition(1, 10_000), map.locate(p))
    }

    @Test
    fun unknownDurationsFallBackToDefault() {
        val unknown = GrooveMap(listOf(0L, 0L))
        assertEquals(2 * GrooveMap.DEFAULT_TRACK_MS, unknown.totalMs)
        assertEquals(listOf(0.5f), unknown.boundaries)
    }
}
