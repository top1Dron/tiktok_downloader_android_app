package com.tiktokdownloader.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "download_history")
data class DownloadHistory(
    @PrimaryKey
    val taskId: String,
    val url: String,
    val fileName: String?,
    val filePath: String?,
    val platform: String, // "tiktok" or "instagram"
    val status: String, // "pending", "downloading", "completed", "failed"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val error: String? = null
)

