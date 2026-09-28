package com.powerfulmedia.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.powerfulmedia.player.data.local.MediaEntity
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlayerViewModel(private val powerfulPlayer: PowerfulPlayer) : ViewModel() {

    private var equalizer: Equalizer? = null

    // Internal setter for testing purposes
    internal fun setEqualizer(eq: Equalizer?) {
        this.equalizer = eq
    }

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    init {
        powerfulPlayer.setOnStateChangedListener { newState ->
            _uiState.update { newState }
            ensureEqualizerInitialized()
        }
    }

    private fun ensureEqualizerInitialized() {
        if (equalizer == null) {
            val sessionId = powerfulPlayer.getAudioSessionId()
            if (sessionId != 0) {
                try {
                    equalizer = Equalizer(0, sessionId).apply {
                        enabled = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Int) {
        val gainMilliBel = (gainDb * 100).toShort()
        equalizer?.setBandLevel(bandIndex.toShort(), gainMilliBel)
    }

    fun updateDebugMetrics(metrics: DebugMetrics) {
        _uiState.update { it.copy(debugMetrics = metrics) }
    }

    fun toggleDebugOverlay() {
        _uiState.update { it.copy(isDebugEnabled = !it.isDebugEnabled) }
    }

    fun play() {
        powerfulPlayer.play()
    }

    fun pause() {
        powerfulPlayer.pause()
    }

    fun seekTo(position: Long) {
        powerfulPlayer.seekTo(position)
    }

    fun setCurrentTrack(track: MediaEntity) {
        powerfulPlayer.prepare(Uri.parse(track.uri))
        _uiState.update {
            it.copy(
                currentTrack = track,
                currentPosition = 0L,
                duration = track.duration,
                isPlaying = false
            )
        }
    }
}
