package com.powerfulmedia.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val duration: Long,
    val dateAdded: Long,
    val dateModified: Long
)
