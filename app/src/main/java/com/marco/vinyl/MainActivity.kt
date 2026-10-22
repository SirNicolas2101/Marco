package com.marco.vinyl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marco.vinyl.fold.ScreenRole
import com.marco.vinyl.fold.rememberScreenRole
import com.marco.vinyl.model.needleProgress
import com.marco.vinyl.ui.library.LibraryScreen
import com.marco.vinyl.ui.player.PlayerScreen
import com.marco.vinyl.ui.theme.VinylTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VinylApplication).container
        setContent {
            VinylTheme {
                val viewModel: VinylViewModel = viewModel(factory = VinylViewModel.factory(container))
                VinylApp(viewModel, rememberScreenRole(this))
            }
        }
    }
}

@Composable
private fun VinylApp(viewModel: VinylViewModel, role: ScreenRole) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    val record = deck.platterRecord
    val progress = needleProgress(deck, playback)
    val playingHere = record != null && playback.albumId == record.id && deck.needleDown
    val currentTrack = if (playingHere) record?.tracks?.getOrNull(playback.trackIndex) else null

    // Während die Nadel aufliegt, soll das Außendisplay nicht ausgehen.
    val view = LocalView.current
    DisposableEffect(deck.needleDown) {
        view.keepScreenOn = deck.needleDown
        onDispose { view.keepScreenOn = false }
    }

    when (role) {
        ScreenRole.Inner -> {
            val selected = deck.library.firstOrNull { it.id == selectedId }
            BackHandler(enabled = selected != null) { selectedId = null }
            LibraryScreen(
                albums = deck.library,
                loading = deck.loading,
                error = deck.error,
                sourceName = deck.sourceName,
                selected = selected,
                pendingRecord = deck.pendingRecord,
                platterRecord = record,
                playingTrack = currentTrack,
                onSelect = { selectedId = it.id },
                onDismissSelection = { selectedId = null },
                onSendToFront = {
                    viewModel.sendToFront(it)
                    selectedId = null
                },
                onLiftNeedle = viewModel::liftNeedle,
            )
        }
        ScreenRole.Cover -> PlayerScreen(
            platterRecord = record,
            pendingRecord = deck.pendingRecord,
            needleProgress = progress,
            currentTrack = currentTrack,
            currentTrackIndex = playback.trackIndex,
            positionMs = playback.positionMs,
            error = deck.error,
            onPlacePending = viewModel::placePendingOnPlatter,
            onRemoveRecord = viewModel::removeFromPlatter,
            onDropNeedle = viewModel::dropNeedle,
            onLiftNeedle = viewModel::liftNeedle,
        )
    }
}
