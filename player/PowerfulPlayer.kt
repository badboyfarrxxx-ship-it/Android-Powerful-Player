package com.powerfulmedia.player

import android.content.Context
import android.net.Uri
import android.view.Surface
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView

class PowerfulPlayer(private val context: Context) : Player.Listener {

    private var exoPlayer: ExoPlayer? = null
    private var onStateChangedListener: ((PlayerState) -> Unit)? = null

    fun initialize() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(this@PowerfulPlayer)
            }
        }
    }

    fun release() {
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        exoPlayer = null
    }

    fun prepare(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun setSurface(surface: Surface) {
        exoPlayer?.setVideoSurface(surface)
    }

    fun setOnStateChangedListener(listener: (PlayerState) -> Unit) {
        this.onStateChangedListener = listener
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateState()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateState()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        updateState()
    }

    private fun updateState() {
        val player = exoPlayer ?: return
        val state = PlayerState(
            isPlaying = player.isPlaying,
            currentPosition = player.currentPosition,
            duration = player.duration,
            playbackState = when (player.playbackState) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> PlaybackState.ENDED
                else -> PlaybackState.IDLE
            }
        )
        onStateChangedListener?.invoke(state)
    }

    fun getAudioSessionId(): Int {
        return exoPlayer?.audioSessionId ?: 0
    }
}
