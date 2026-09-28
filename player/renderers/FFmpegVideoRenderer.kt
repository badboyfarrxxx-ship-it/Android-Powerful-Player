package com.powerfulmedia.player.renderers

import android.content.Context
import android.view.Surface
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.decoder.DecoderInputBuffer
import com.google.android.exoplayer2.format.Format
import com.google.android.exoplayer2.source.SampleStream
import com.powerfulmedia.nativeapi.FFmpegBridge
import com.powerfulmedia.nativeapi.RingBuffer

/**
 * FFmpegVideoRenderer bypasses MediaCodec and uses the native FFmpeg engine
 * via the FFmpegBridge.
 */
class FFmpegVideoRenderer(
    private val context: Context,
    private val factory: CustomRenderersFactory? = null
) : Renderer {

    private var surface: Surface? = null
    private var isInitialized = false
    private var trackId = -1
    private var playbackPositionUs = 0L

    private val ffmpegBridge = FFmpegBridge()
    private val ringBuffer = RingBuffer()

    // In a real ExoPlayer implementation, samples are provided via a MediaCodec
    // or a custom decoder. Since we are a software renderer, we need a way
    // to pull raw packets from the player's source.
    private var sampleStream: SampleStream? = null

    override fun supportsFormat(format: Format): Boolean {
        val mimeType = format.sampleMimeType ?: return false

        // The Hybrid Switch: Only support if hardware decoder is unavailable.
        // We check this via the factory logic.
        val hardwareAvailable = factory?.isHardwareDecoderAvailable(mimeType) ?: false
        if (hardwareAvailable) {
            return false
        }

        // FFmpeg handles almost everything.
        return mimeType.startsWith("video/")
    }

    override fun onEnabled(playbackPositionUs: Long) {
        this.playbackPositionUs = playbackPositionUs
    }

    override fun onPositionReset(playbackPositionUs: Long, offset: Boolean) {
        this.playbackPositionUs = playbackPositionUs
        ffmpegBridge.flush()
        ringBuffer.clear()
    }

    override fun render(positionUs: Long, elapsedRealtimeUs: Long) {
        if (!isInitialized) return

        this.playbackPositionUs = positionUs

        // 1. Data Pipeline: Pull raw packets from the stream and push to FFmpeg.
        // In a functional implementation, we read as many samples as the decoder can handle.
        while (sampleStream?.readSampleData() == true) {
            val packet = sampleStream?.getCurrentSample()
            if (packet != null) {
                ffmpegBridge.decodePacket(packet)
            }
        }

        // 2. AV Sync & Clock Management:
        // Pop frames from the RingBuffer only when their timestamp <= positionUs.
        while (ringBuffer.hasAvailableFrames()) {
            val frame = ringBuffer.peekFrame() ?: break

            if (frame.timestampUs <= positionUs) {
                // It is time to render this frame.
                val decodedFrame = ringBuffer.pollFrame()
                renderFrameToSurface(decodedFrame)
            } else {
                // Frame is for the future; stop popping.
                break
            }
        }
    }

    private fun renderFrameToSurface(frame: Any) {
        surface?.let {
            // Native call to push decoded YUV/RGB frame to Android Surface.
            ffmpegBridge.renderToSurface(frame, it)
        }
    }

    override fun setSurface(surface: Surface) {
        this.surface = surface
    }

    override fun flush() {
        ffmpegBridge.flush()
        ringBuffer.clear()
    }

    override fun release() {
        // Critical: Destroy native decoder to prevent memory leaks.
        ffmpegBridge.release()
        ringBuffer.release()
        isInitialized = false
    }

    override fun isReady(): Boolean = isInitialized
    override fun isEnded(): Boolean = false
    override fun getPlaybackPosition(): Long = playbackPositionUs

    // These would be called by the player to set up the stream.
    fun initDecoder(trackId: Int, stream: SampleStream) {
        this.trackId = trackId
        this.sampleStream = stream
        ffmpegBridge.initDecoder()
        isInitialized = true
    }
}
