package com.powerfulmedia.player.data.repository

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.powerfulmedia.player.data.local.MediaDao
import com.powerfulmedia.player.data.local.MediaEntity
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MediaRepositoryTest {
    private lateinit var contentResolver: ContentResolver
    private lateinit var mediaDao: MediaDao
    private lateinit var repository: MediaRepository

    @Before
    fun setup() {
        contentResolver = mockk(relaxed = true)
        mediaDao = mockk(relaxed = true)
        repository = MediaRepository(contentResolver, mediaDao)
    }

    @Test
    fun `getMediaFiles scans and returns media from MediaStore`() = runBlocking {
        // Mock Cursor
        val mockCursor = mockk<android.database.Cursor>()
        every { mockCursor.use(any()) } answers {
            val block = firstArg<() -> Unit>()
            block()
            mockCursor
        }

        // Setup column indices
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID) } returns 0
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME) } returns 1
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE) } returns 2
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE) } returns 3
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION) } returns 4
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED) } returns 5
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED) } returns 6

        // Mock cursor navigation (1 item)
        every { mockCursor.moveToNext() } returnsMany listOf(true, false)
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getString(1) } returns "Test Song.mp3"
        every { mockCursor.getString(2) } returns "audio/mpeg"
        every { mockCursor.getLong(3) } returns 1024L
        every { mockCursor.getLong(4) } returns 200L
        every { mockCursor.getLong(5) } returns 1000L
        every { mockCursor.getLong(6) } returns 1000L

        every { contentResolver.query(any(), any(), any(), any(), any()) } returns mockCursor

        val mockList = listOf(
            MediaEntity(123L, "content://media/external/audio/media/123", "Test Song.mp3", "audio/mpeg", 1024L, 200L, 1000L, 1000L)
        )

        // Using a flow mock or simple list for the DAO
        coEvery { mediaDao.getAllMedia() } returns kotlinx.coroutines.flow.flowOf(mockList)

        val result = repository.getMediaFiles().first()

        assertEquals(1, result.size)
        assertEquals("Test Song.mp3", result[0].displayName)
        coVerify { mediaDao.clearAll() }
        coVerify { mediaDao.insertAll(any()) }
    }
}
