package com.marco.vinyl.model

import com.marco.vinyl.turntable.GrooveMap

/** Zustand von Schrank und Plattenspieler, geteilt zwischen Innen- und Außendisplay. */
data class DeckState(
    val library: List<Album> = emptyList(),
    val loading: Boolean = true,
    val sourceName: String = "",
    /** Nach vorne geschickt, liegt neben dem Plattenspieler und wartet aufs Auflegen. */
    val pendingRecord: Album? = null,
    val platterRecord: Album? = null,
    val needleDown: Boolean = false,
    /** Stelle (0 = Außenrand, 1 = Auslaufrille), an der die Nadel zuletzt abgesetzt wurde. */
    val dropProgress: Float = 0f,
    val error: String? = null,
)

data class PlaybackState(
    val albumId: String? = null,
    val trackIndex: Int = 0,
    val positionMs: Long = 0,
    val isPlaying: Boolean = false,
)

/**
 * Wo die Nadel gerade auf der Platte steht (0..1), oder `null`, wenn sie angehoben ist.
 * Solange der Player noch lädt, bleibt sie an der Absetzstelle.
 */
fun needleProgress(deck: DeckState, playback: PlaybackState): Float? {
    val record = deck.platterRecord ?: return null
    if (!deck.needleDown) return null
    return if (playback.albumId == record.id && playback.isPlaying) {
        GrooveMap.of(record).progressOf(playback.trackIndex, playback.positionMs)
    } else {
        deck.dropProgress
    }
}
