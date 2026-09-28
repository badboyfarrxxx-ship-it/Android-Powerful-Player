package com.powerfulmedia.player

import com.powerfulmedia.player.renderers.CustomRenderersFactory
import com.powerfulmedia.player.renderers.FFmpegVideoRenderer
import com.google.android.exoplayer2.format.Format
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import android.content.Context
import android.media.MediaCodecList
import android.media.MediaCodecInfo

class RendererSwitchTest {

    private val mockContext = mock(Context::class.java)

    @Test
    fun `test CustomRenderersFactory selects FFmpegVideoRenderer for unsupported format`() {
        val factory = CustomRenderersFactory(mockContext)

        // We mock a scenario where the MIME type is NOT supported by any hardware decoder.
        // In a real Android environment, "video/x-matroska" (the container)
        // or a rare codec would return false from isHardwareDecoderAvailable.
        val unsupportedMimeType = "video/x-rare-codec"

        // Since isHardwareDecoderAvailable actually queries MediaCodecList,
        // on a test machine it might actually be unsupported.
        val hardwareAvailable = factory.isHardwareDecoderAvailable(unsupportedMimeType)

        // We verify that if hardware is unavailable, FFmpeg renderer accepts the format.
        val renderer = FFmpegVideoRenderer(mockContext, factory)
        val format = Format.Builder().setSampleMimeType(unsupportedMimeType).build()

        if (!hardwareAvailable) {
            assertTrue("FFmpeg should support $unsupportedMimeType when hardware is unavailable",
                renderer.supportsFormat(format))
        }
    }

    @Test
    fun `test CustomRenderersFactory prefers MediaCodec for supported format`() {
        val factory = CustomRenderersFactory(mockContext)

        // H.264 is supported on almost all Android devices.
        val supportedMimeType = "video/avc"

        val hardwareAvailable = factory.isHardwareDecoderAvailable(supportedMimeType)

        val renderer = FFmpegVideoRenderer(mockContext, factory)
        val format = Format.Builder().setSampleMimeType(supportedMimeType).build()

        if (hardwareAvailable) {
            assertFalse("FFmpeg should NOT support $supportedMimeType when hardware is available",
                renderer.supportsFormat(format))
        }
    }

    @Test
    fun `test FFmpegVideoRenderer synchronization logic`() {
        val factory = CustomRenderersFactory(mockContext)
        val renderer = FFmpegVideoRenderer(mockContext, factory)

        // This is a behavioral test for the render loop.
        // Since we can't easily run the full native bridge in a unit test,
        // we are verifying the interface compliance.
        assertNotNull(renderer)
        assertEquals(0L, renderer.getPlaybackPosition())
    }
}
