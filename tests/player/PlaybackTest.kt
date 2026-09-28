package com.powerfulmedia.player

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.android.exoplayer2.Player
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaybackTest {

    private lateinit var context: Context
    private lateinit var powerfulPlayer: PowerfulPlayer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        powerfulPlayer = PowerfulPlayer(context)
        powerfulPlayer.initialize()
    }

    @After
    fun teardown() {
        powerfulPlayer.release()
    }

    @Test
    fun testPrepareValidUri_SetsStateToReady() {
        val testUri = Uri.parse("file:///android_asset/test_media.mp3")
        val latch = CountDownLatch(1)

        powerfulPlayer.setOnStateChangedListener { state ->
            if (state == Player.STATE_READY) {
                latch.countDown()
            }
        }

        powerfulPlayer.prepare(testUri)

        val ready = latch.await(5, TimeUnit.SECONDS)
        assertEquals("Player failed to reach STATE_READY within timeout", true, ready)
        assertEquals(Player.STATE_READY, powerfulPlayer.getPlaybackState())
    }
}
