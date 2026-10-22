package com.marco.vinyl.model

/**
 * Quelle für den Plattenschrank. Aktuell gibt es nur [com.marco.vinyl.data.demo.DemoMusicRepository];
 * eine SoundCloud-Anbindung implementiert später dasselbe Interface.
 */
interface MusicRepository {
    /** Anzeigename der Quelle, z. B. „Demo“ oder „SoundCloud“. */
    val sourceName: String

    suspend fun loadLibrary(): List<Album>

    /** Liefert für jeden Titel des Albums eine abspielbare URI (Datei oder Stream), in Titelreihenfolge. */
    suspend fun resolveStreams(album: Album): List<String>
}
