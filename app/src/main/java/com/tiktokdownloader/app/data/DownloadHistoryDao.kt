package com.tiktokdownloader.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {
    @Query("SELECT * FROM download_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<DownloadHistory>>
    
    @Query("SELECT * FROM download_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<DownloadHistory>>
    
    @Query("SELECT * FROM download_history WHERE taskId = :taskId")
    suspend fun getByTaskId(taskId: String): DownloadHistory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: DownloadHistory)
    
    @Update
    suspend fun update(history: DownloadHistory)
    
    @Delete
    suspend fun delete(history: DownloadHistory)
    
    @Query("DELETE FROM download_history")
    suspend fun deleteAll()
}

