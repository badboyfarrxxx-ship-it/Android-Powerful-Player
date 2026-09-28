package com.powerfulmedia.player.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import com.powerfulmedia.player.PlayerViewModel
import com.powerfulmedia.player.ui.components.GestureOverlay
import com.powerfulmedia.player.ui.components.SystemServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class GestureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val mockViewModel = mockk<PlayerViewModel>(relaxed = true)
    private val mockAudioManager = mockk<AudioManager>(relaxed = true)
    private val mockSystemServiceProvider = mockk<SystemServiceProvider>()

    init {
        every { mockSystemServiceProvider.getAudioManager() } returns mockAudioManager
    }

    @Test
    fun testVolumeSwipeUp_IncreasesVolume() {
        composeTestRule.setContent {
            GestureOverlay(
                viewModel = mockViewModel,
                systemServiceProvider = mockSystemServiceProvider
            ) {
                // Simple content to fill screen
            }
        }

        // Swipe up on the left half of the screen
        composeTestRule.onRoot().performTouchInput {
            swipeUp(startY = 1000f, endY = 100f, startX = 100f, endX = 100f)
        }

        // Verify AudioManager.adjustStreamVolume was called with ADJUST_RAISE
        verify {
            mockAudioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                0
            )
        }
    }

    @Test
    fun testVolumeSwipeDown_DecreasesVolume() {
        composeTestRule.setContent {
            GestureOverlay(
                viewModel = mockViewModel,
                systemServiceProvider = mockSystemServiceProvider
            ) {
                // Simple content to fill screen
            }
        }

        // Swipe down on the left half of the screen
        composeTestRule.onRoot().performTouchInput {
            swipeDown(startY = 100f, endY = 1000f, startX = 100f, endX = 100f)
        }

        // Verify AudioManager.adjustStreamVolume was called with ADJUST_LOWER
        verify {
            mockAudioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                0
            )
        }
    }

    @Test
    fun testBrightnessSwipeUp_ChangesBrightness() {
        val activity = composeTestRule.activity
        val initialBrightness = activity.window.attributes.screenBrightness

        composeTestRule.setContent {
            GestureOverlay(
                viewModel = mockViewModel,
                systemServiceProvider = mockSystemServiceProvider
            ) {
                // Simple content to fill screen
            }
        }

        // Swipe up on the right half of the screen
        composeTestRule.onRoot().performTouchInput {
            swipeUp(startY = 1000f, endY = 100f, startX = 1000f, endX = 1000f)
        }

        val finalBrightness = activity.window.attributes.screenBrightness
        assertNotEquals("Brightness should have changed after swipe", initialBrightness, finalBrightness)
    }
}
