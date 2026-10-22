package com.marco.vinyl.model

/**
 * Eine Platte im Schrank. Für die Demo-Daten wie später für SoundCloud gleich:
 * eine SoundCloud-Playlist bzw. ein SoundCloud-Album wird zu einem [Album].
 */
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val tracks: List<Track>,
    val sleeve: SleeveStyle,
    /** Cover-Bild (z. B. SoundCloud `artwork_url`). Ohne URL wird das Cover aus [sleeve] gezeichnet. */
    val artworkUrl: String? = null,
)

data class Track(
    val id: String,
    val title: String,
    val durationMs: Long,
)

/** Farben (ARGB) und Muster, aus denen Hülle, Rücken und Label gezeichnet werden. */
data class SleeveStyle(
    val primary: Long,
    val secondary: Long,
    val pattern: SleevePattern,
)

enum class SleevePattern { Circles, Stripes, Grid, Sunburst, Waves }
