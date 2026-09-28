package com.powerfulmedia.player.audio

import android.media.audiofx.Equalizer
import com.powerfulmedia.player.PlayerViewModel
import com.powerfulmedia.player.PowerfulPlayer
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EqTest {

    @Test
    fun `test viewModel converts dB to milliBels for equalizer`() {
        // Setup mocks
        val mockPlayer = mock<PowerfulPlayer>()
        val mockEqualizer = mock<Equalizer>()

        // Mock audio session ID to ensure equalizer initialization
        whenever(mockPlayer.getAudioSessionId()).thenReturn(123)

        val viewModel = PlayerViewModel(mockPlayer)

        // Inject the mockEqualizer into the ViewModel for verification
        viewModel.setEqualizer(mockEqualizer)

        val bandIndex = 2
        val gainDb = 5 // 5 dB
        val expectedMilliBel = (5 * 100).toShort()

        // Act: Set gain in dB via ViewModel
        viewModel.setBandGain(bandIndex, gainDb)

        // Verify: The Equalizer API receives the correctly converted milliBel value
        verify(mockEqualizer).setBandLevel(bandIndex.toShort(), expectedMilliBel)
    }
}
