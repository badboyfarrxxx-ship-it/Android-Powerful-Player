package com.example.powerfulplayer

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Native bridge class to interface with FFmpeg C++ engine.
 */
class FFmpegBridge {
    companion object {
        init {
            System.loadLibrary("powerful_player_jni")
        }
    }

    external fun ping(): Int
    external fun decodePacket(formatContextPtr: Long, packetPtr: Long): Int
    external fun releaseBuffer(bufferPtr: Long)
}

/**
 * Test suite for verifying the JNI bridge connectivity.
 */
class JniPingTest {
    private lateinit var bridge: FFmpegBridge

    @Before
    fun setUp() {
        bridge = FFmpegBridge()
    }

    @Test
    fun testNativePingReturnsOne() {
        // Verify that the native library is loaded and the ping() method returns 1.
        val result = bridge.ping()
        assertEquals("The native ping() method should return 1 to verify connectivity", 1, result)
    }
}
