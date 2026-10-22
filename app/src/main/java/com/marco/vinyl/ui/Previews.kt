package com.marco.vinyl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.marco.vinyl.data.demo.DemoMusicRepository
import com.marco.vinyl.ui.library.LibraryScreen
import com.marco.vinyl.ui.player.PlayerScreen
import com.marco.vinyl.ui.theme.VinylTheme

// Ungefähre Maße eines Galaxy Z Fold: Innendisplay fast quadratisch, Außendisplay schmal und hoch.
private const val INNER = "spec:width=720dp,height=820dp,dpi=420"
private const val COVER = "spec:width=400dp,height=900dp,dpi=420"

private val albums = DemoMusicRepository.albums

@Preview(name = "Innen – Schrank", device = INNER)
@Composable
private fun LibraryPreview() = VinylTheme {
    LibraryScreen(
        albums = albums,
        loading = false,
        error = null,
        sourceName = "Demo",
        selected = null,
        pendingRecord = albums[1],
        platterRecord = null,
        playingTrack = null,
        onSelect = {},
        onDismissSelection = {},
        onSendToFront = {},
        onLiftNeedle = {},
    )
}

@Preview(name = "Innen – Hülle geöffnet", device = INNER)
@Composable
private fun SleevePreview() = VinylTheme {
    LibraryScreen(
        albums = albums,
        loading = false,
        error = null,
        sourceName = "Demo",
        selected = albums[0],
        pendingRecord = null,
        platterRecord = null,
        playingTrack = null,
        onSelect = {},
        onDismissSelection = {},
        onSendToFront = {},
        onLiftNeedle = {},
    )
}

@Preview(name = "Außen – Platte wartet", device = COVER)
@Composable
private fun PendingPreview() = VinylTheme {
    PlayerScreen(
        platterRecord = null,
        pendingRecord = albums[2],
        needleProgress = null,
        currentTrack = null,
        currentTrackIndex = 0,
        positionMs = 0,
        error = null,
        onPlacePending = {},
        onRemoveRecord = {},
        onDropNeedle = {},
        onLiftNeedle = {},
    )
}

@Preview(name = "Außen – spielt", device = COVER)
@Composable
private fun PlayingPreview() = VinylTheme {
    val album = albums[3]
    PlayerScreen(
        platterRecord = album,
        pendingRecord = null,
        needleProgress = 0.35f,
        currentTrack = album.tracks[1],
        currentTrackIndex = 1,
        positionMs = 12_000,
        error = null,
        onPlacePending = {},
        onRemoveRecord = {},
        onDropNeedle = {},
        onLiftNeedle = {},
    )
}
