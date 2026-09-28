package com.powerfulmedia.player.renderers

import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioRenderer
import com.google.android.exoplayer2.video.VideoRenderer
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import android.content.Context
import android.media.MediaCodecList
import android.os.Handler
import com.google.android.exoplayer2.source.SampleStream

class CustomRenderersFactory(context: Context) : DefaultRenderersFactory(context) {

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        allowedVideoJoiningTypes: Int,
        out: ArrayList<Renderer>
    ) {
        // We first build the standard Renderers.
        super.buildVideoRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            allowedVideoJoiningTypes,
            out
        )

        // We add the FFmpegVideoRenderer as a fallback.
        // In ExoPlayer, multiple renderers can be provided, and the player
        // will choose the first one that supports the format.
        // To ensure FFmpeg is only used when MediaCodec fails,
        // the FFmpegVideoRenderer.supportsFormat() should be carefully implemented
        // or we can rely on the order.
        // However, to strictly follow the requirement of "Hybrid Switch",
        // we add it here. The actual selection happens in supportsFormat.
        out.add(FFmpegVideoRenderer(context))
    }

    /**
     * Determines if the system hardware decoder supports the given MIME type.
     * This is used by the renderer to decide if it should handle the stream.
     */
    fun isHardwareDecoderAvailable(mimeType: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos
        for (info in codecInfos) {
            if (info.isEncoder) continue
            val types = info.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
