package com.powerfulmedia.player

import com.powerfulmedia.player.data.local.MediaEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @Test
    fun `testPlayPauseTransition`() = runTest {
        val viewModel = PlayerViewModel()

        // Initial state should be not playing
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.play()
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.pause()
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `testSeekToUpdatesPosition`() = runTest {
        val viewModel = PlayerViewModel()
        val position = 1000L

        viewModel.seekTo(position)

        assertEquals(position, viewModel.uiState.value.currentPosition)
    }

    @Test
    fun `testSetCurrentTrackUpdatesState`() = runTest {
        val viewModel = PlayerViewModel()
        val track = MediaEntity(
            id = 1L,
            uri = "test://uri",
            displayName = "Test Track",
            mimeType = "audio/mpeg",
            size = 1024L,
            duration = 5000L,
            dateAdded = 123L,
            dateModified = 456L
        )

        viewModel.setCurrentTrack(track)

        assertEquals(track, viewModel.uiState.value.currentTrack)
        assertEquals(0L, viewModel.uiState.value.currentPosition)
        assertEquals(track.duration, viewModel.uiState.value.duration)
        assertFalse(viewModel.uiState.value.isPlaying)
    }
}
