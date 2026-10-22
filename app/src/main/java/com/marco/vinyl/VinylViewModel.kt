package com.marco.vinyl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marco.vinyl.model.Album
import com.marco.vinyl.model.DeckState
import com.marco.vinyl.model.MusicRepository
import com.marco.vinyl.model.PlaybackState
import com.marco.vinyl.playback.PlayerController
import com.marco.vinyl.turntable.GrooveMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gemeinsamer Zustand für beide Displays. Weil die Activity beim Falten nicht neu erstellt wird
 * (siehe `configChanges` im Manifest) und der ViewModel ohnehin überlebt, wandert alles mit.
 */
class VinylViewModel(
    private val repository: MusicRepository,
    private val player: PlayerController,
) : ViewModel() {

    private val _deck = MutableStateFlow(DeckState(sourceName = repository.sourceName))
    val deck: StateFlow<DeckState> = _deck.asStateFlow()

    val playback: StateFlow<PlaybackState> = player.state

    /** Streams der Platte auf dem Teller; werden beim Auflegen schon vorbereitet. */
    private var streams: Deferred<List<String>>? = null
    private var dropJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val albums = repository.loadLibrary()
                _deck.update { it.copy(library = albums, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _deck.update { it.copy(loading = false, error = e.message ?: "Bibliothek konnte nicht geladen werden") }
            }
        }
        viewModelScope.launch {
            // Automatische Endabschaltung: am Ende der Platte hebt sich die Nadel.
            player.endOfRecord.collect { albumId ->
                if (_deck.value.platterRecord?.id == albumId) _deck.update { it.copy(needleDown = false) }
            }
        }
    }

    fun sendToFront(album: Album) {
        _deck.update { it.copy(pendingRecord = album) }
    }

    fun placePendingOnPlatter() {
        val record = _deck.value.pendingRecord ?: return
        dropJob?.cancel()
        player.stop()
        streams?.cancel()
        streams = viewModelScope.async { repository.resolveStreams(record) }
        _deck.update {
            it.copy(platterRecord = record, pendingRecord = null, needleDown = false, dropProgress = 0f, error = null)
        }
    }

    fun removeFromPlatter() {
        dropJob?.cancel()
        player.stop()
        streams?.cancel()
        streams = null
        _deck.update { it.copy(platterRecord = null, needleDown = false) }
    }

    fun dropNeedle(progress: Float) {
        val record = _deck.value.platterRecord ?: return
        val pending = streams ?: return
        val position = GrooveMap.of(record).locate(progress)
        _deck.update { it.copy(needleDown = true, dropProgress = progress) }

        dropJob?.cancel()
        dropJob = viewModelScope.launch {
            val uris = try {
                pending.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _deck.update { it.copy(needleDown = false, error = e.message ?: "Platte konnte nicht geladen werden") }
                return@launch
            }
            val now = _deck.value
            if (now.platterRecord?.id != record.id || !now.needleDown) return@launch
            player.load(record.id, uris)
            player.playFrom(position.trackIndex, position.positionMs)
        }
    }

    fun liftNeedle() {
        dropJob?.cancel()
        player.pause()
        _deck.update { it.copy(needleDown = false) }
    }

    override fun onCleared() {
        player.stop()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { VinylViewModel(container.repository, container.player) }
        }
    }
}
