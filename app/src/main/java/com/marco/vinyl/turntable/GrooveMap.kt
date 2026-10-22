package com.marco.vinyl.turntable

import com.marco.vinyl.model.Album

/**
 * Bildet die Titel eines Albums auf die Rille ab: 0 = Einlaufrille am Außenrand, 1 = Auslaufrille.
 *
 * Wie bei echtem Vinyl (konstante Drehzahl, konstanter Rillenabstand) wandert die Nadel
 * linear mit der Zeit nach innen, längere Titel belegen also breitere Ringe.
 */
class GrooveMap(durationsMs: List<Long>) {

    private val durations: List<Long> =
        durationsMs.ifEmpty { listOf(DEFAULT_TRACK_MS) }.map { if (it > 0) it else DEFAULT_TRACK_MS }

    private val starts: LongArray = LongArray(durations.size).also { s ->
        for (i in 1 until durations.size) s[i] = s[i - 1] + durations[i - 1]
    }

    val totalMs: Long = durations.sum()

    val trackCount: Int get() = durations.size

    /** Fortschritt an den Übergängen zwischen zwei Titeln, dort sind die Rillen-Lücken sichtbar. */
    val boundaries: List<Float> = (1 until durations.size).map { starts[it].toFloat() / totalMs }

    /** Welcher Titel und welche Stelle beim Absetzen der Nadel bei [progress] gespielt wird. */
    fun locate(progress: Float): GroovePosition {
        val t = (progress.coerceIn(0f, 1f) * totalMs).toLong().coerceIn(0, totalMs - 1)
        var index = starts.indexOfLast { it <= t }
        var position = t - starts[index]
        when {
            // In der Lücke vor einem Titel: Nadel läuft in den Titelanfang.
            position < LEAD_IN_MS -> position = 0
            // Kurz vor Titelende abgesetzt: gleich beim nächsten Titel beginnen.
            durations[index] - position < LEAD_OUT_MS && index < durations.lastIndex -> {
                index++
                position = 0
            }
        }
        return GroovePosition(index, position)
    }

    fun progressOf(trackIndex: Int, positionMs: Long): Float {
        val index = trackIndex.coerceIn(0, durations.lastIndex)
        val t = starts[index] + positionMs.coerceIn(0, durations[index])
        return (t.toFloat() / totalMs).coerceIn(0f, 1f)
    }

    companion object {
        const val DEFAULT_TRACK_MS = 180_000L
        const val LEAD_IN_MS = 1_500L
        const val LEAD_OUT_MS = 1_000L

        fun of(album: Album): GrooveMap = GrooveMap(album.tracks.map { it.durationMs })
    }
}

data class GroovePosition(val trackIndex: Int, val positionMs: Long)
