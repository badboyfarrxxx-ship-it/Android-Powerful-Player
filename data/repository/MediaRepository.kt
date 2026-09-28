package com.powerfulmedia.player.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Insets
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.powerfulmedia.player.data.local.MediaDao
import com.powerfulmedia.player.data.local.MediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class MediaRepository(
    private val contentResolver: ContentResolver,
    private val mediaDao: MediaDao
) {
    private val TAG = "MediaRepository"

    fun getMediaFiles(): Flow<List<MediaEntity>> = flow {
        scanMediaFiles()
        emitAll(mediaDao.getAllMedia())
    }.flowOn(Dispatchers.IO)

    private suspend fun scanMediaFiles() {
        val mediaList = mutableListOf<MediaEntity>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                // CRITICAL: persistableUriPermission for Android 11+ (Scoped Storage)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            contentUri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to take persistable permission for $contentUri: ${e.message}")
                    }
                }

                mediaList.add(
                    MediaEntity(
                        id = id,
                        uri = contentUri.toString(),
                        displayName = cursor.getString(nameColumn) ?: "Unknown",
                        mimeType = cursor.getString(mimeColumn) ?: "unknown/unknown",
                        size = cursor.getLong(sizeColumn),
                        duration = cursor.getLong(durationColumn),
                        dateAdded = cursor.getLong(dateAddedColumn),
                        dateModified = cursor.getLong(dateModifiedColumn)
                    )
                )
            }
        }

        mediaDao.clearAll()
        mediaDao.insertAll(mediaList)
    }
}

// Helper extension to mimic flow emitAll in simple implementation
private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
    kotlinx.coroutines.flow.collect { value ->
        emit(value)
    }
}
