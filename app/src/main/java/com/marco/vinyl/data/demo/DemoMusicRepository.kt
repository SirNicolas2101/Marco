package com.marco.vinyl.data.demo

import com.marco.vinyl.model.Album
import com.marco.vinyl.model.MusicRepository
import com.marco.vinyl.model.SleevePattern
import com.marco.vinyl.model.SleeveStyle
import com.marco.vinyl.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fiktive Platten zum Ausprobieren von Schrank, Gesten und Plattenspieler.
 * Die Musik wird beim ersten Auflegen erzeugt und in [cacheDir] abgelegt.
 */
class DemoMusicRepository(private val cacheDir: File) : MusicRepository {

    override val sourceName = "Demo"

    override suspend fun loadLibrary(): List<Album> = albums

    override suspend fun resolveStreams(album: Album): List<String> = withContext(Dispatchers.IO) {
        val dir = File(cacheDir, "demo-audio").apply { mkdirs() }
        album.tracks.map { track ->
            val file = File(dir, "${track.id}.wav")
            if (!file.exists() || file.length() == 0L) {
                val pcm = DemoAudioSynth.render(seed = track.id.hashCode().toLong(), durationMs = track.durationMs)
                val tmp = File(dir, "${track.id}.tmp")
                tmp.writeBytes(DemoAudioSynth.wav(pcm))
                tmp.renameTo(file)
            }
            "file://${file.absolutePath}"
        }
    }

    companion object {
        val albums: List<Album> = listOf(
            demo("Mitternachtsfahrt", "Die Nachtfalter", 1979, 0xFF1B2A4A, 0xFFE8B04B, SleevePattern.Sunburst,
                "Auffahrt", "Tankstelle 3 Uhr", "Rücklichter", "Heimweg"),
            demo("Salt & Static", "Harbor Lights", 1984, 0xFF2F6F73, 0xFFF2E3C6, SleevePattern.Waves,
                "Low Tide", "Lighthouse Hum", "Fog Signal", "Driftwood", "Last Ferry"),
            demo("Blauer Montag", "Kassettendeck", 1991, 0xFF2D4FA3, 0xFFF5F1E8, SleevePattern.Stripes,
                "Wecker aus", "Kaffee schwarz", "Bürofenster", "Feierabend"),
            demo("Neon Orchard", "Velvet Tram", 1986, 0xFF3A1F4F, 0xFFFF6FA8, SleevePattern.Grid,
                "Pink Harvest", "Arcade Bloom", "Chrome Petals", "Night Shift"),
            demo("Wolken über Wedding", "Frau Schneider", 1997, 0xFFB9B4A8, 0xFF3C3A36, SleevePattern.Circles,
                "Hinterhof", "Ringbahn", "Späti", "Nieselregen", "Balkonien"),
            demo("Low Tide Radio", "The Lanterns", 1973, 0xFFC4572E, 0xFFF4D9A6, SleevePattern.Sunburst,
                "Station Identification", "Sandbar", "Static Love", "Sign Off"),
            demo("Kupfer", "Ostwind Trio", 1968, 0xFF8C4A2F, 0xFFE9C79A, SleevePattern.Circles,
                "Patina", "Glut", "Draht", "Grünspan"),
            demo("Slow Motion Summer", "Polaroid Club", 2003, 0xFFF2C14E, 0xFF2B6F8E, SleevePattern.Waves,
                "Sunscreen", "Pool Ladder", "Instant Film", "Late August", "Goodbye Tan"),
            demo("Analog Herz", "Röhrenradio", 1982, 0xFF9E1B32, 0xFFF0E6D2, SleevePattern.Grid,
                "Aufwärmen", "Mittelwelle", "Rauschen", "Senderwechsel"),
            demo("Glasshouse Sessions", "Glasshouse Ensemble", 2011, 0xFF4F7F52, 0xFFE4F0D0, SleevePattern.Stripes,
                "Morning Mist", "Fern", "Condensation", "Greenhouse Glow"),
            demo("Sonntagsdienst", "Die Plattenwechsler", 1988, 0xFF5B5F97, 0xFFFFC145, SleevePattern.Circles,
                "Frühschicht", "Kantine", "Pause", "Übergabe", "Dienstschluss"),
            demo("Northern Drift", "Aurora Unit", 2019, 0xFF0F3D3E, 0xFF7FFFD4, SleevePattern.Sunburst,
                "Polar Night", "Ice Road", "Green Veil", "Thaw"),
        )

        private fun demo(
            title: String,
            artist: String,
            year: Int,
            primary: Long,
            secondary: Long,
            pattern: SleevePattern,
            vararg trackTitles: String,
        ): Album {
            val id = "demo-" + title.lowercase().filter { it.isLetterOrDigit() }
            return Album(
                id = id,
                title = title,
                artist = artist,
                year = year,
                tracks = trackTitles.mapIndexed { i, name ->
                    Track(id = "$id-$i", title = name, durationMs = 32_000L + (name.hashCode() and 0x7FFF) % 18 * 1_000L)
                },
                sleeve = SleeveStyle(primary, secondary, pattern),
            )
        }
    }
}
