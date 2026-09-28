package com.powerfulmedia.player

import com.powerfulmedia.player.data.local.MediaEntity

enum class PlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

data class DebugMetrics(
    val fps: Float = 0f,
    val bitrateKbps: Int = 0,
    val syncDriftUs: Long = 0L,
    val decodeSpeedMs: Float = 0f,
    val droppedFrames: Int = 0
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrack: MediaEntity? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isDebugEnabled: Boolean = false,
    val debugMetrics: DebugMetrics = DebugMetrics()
)
