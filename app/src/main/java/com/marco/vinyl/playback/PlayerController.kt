package com.marco.vinyl.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.marco.vinyl.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hält den ExoPlayer app-weit, damit die Musik beim Auf- und Zuklappen weiterläuft.
 * Die aktuelle Position wird während der Wiedergabe regelmäßig veröffentlicht, damit der Tonarm folgen kann.
 */
class PlayerController(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _endOfRecord = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Meldet die Album-ID, wenn die letzte Rille erreicht ist. */
    val endOfRecord: SharedFlow<String> = _endOfRecord.asSharedFlow()

    private var loadedAlbumId: String? = null
    private var ticker: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish()
                if (isPlaying) startTicker() else ticker?.cancel()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) loadedAlbumId?.let { _endOfRecord.tryEmit(it) }
                publish()
            }
        })
    }

    fun load(albumId: String, uris: List<String>) {
        if (albumId == loadedAlbumId) return
        player.setMediaItems(uris.map { MediaItem.fromUri(it) })
        player.prepare()
        loadedAlbumId = albumId
        publish()
    }

    fun playFrom(trackIndex: Int, positionMs: Long) {
        player.seekTo(trackIndex, positionMs)
        player.play()
    }

    fun pause() = player.pause()

    fun stop() {
        player.stop()
        player.clearMediaItems()
        loadedAlbumId = null
        publish()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(TICK_MS)
            }
        }
    }

    private fun publish() {
        _state.value = PlaybackState(
            albumId = loadedAlbumId,
            trackIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition,
            isPlaying = player.isPlaying,
        )
    }

    private companion object {
        const val TICK_MS = 50L
    }
}
