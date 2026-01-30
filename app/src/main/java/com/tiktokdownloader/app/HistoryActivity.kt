package com.tiktokdownloader.app

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tiktokdownloader.app.data.AppDatabase
import com.tiktokdownloader.app.data.DownloadHistory
import com.tiktokdownloader.app.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: AppDatabase
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var tiktokDownloader: PythonTikTokDownloader
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Python runtime for TikTok downloader
        PythonTikTokDownloader.initializePython(this)
        
        // Initialize Python TikTok downloader with cache directory
        val cacheDir = File(cacheDir, "tiktok_downloads")
        cacheDir.mkdirs()
        tiktokDownloader = PythonTikTokDownloader(cacheDir)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Initialize database
        database = AppDatabase.getDatabase(this)
        
        // Setup RecyclerView
        historyAdapter = HistoryAdapter(
            onReloadClick = { history ->
                reloadVideo(history)
            }
        )
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = historyAdapter
        
        // Setup clear history button
        binding.clearHistoryButton.setOnClickListener {
            showClearHistoryDialog()
        }
        
        loadHistory()
    }
    
    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all download history? This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearAllHistory() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.downloadHistoryDao().deleteAll()
                }
                Toast.makeText(this@HistoryActivity, "History cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error clearing history", e)
                Toast.makeText(this@HistoryActivity, "Error clearing history", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadHistory() {
        val filterPlatform = intent.getStringExtra("platform")
        
        lifecycleScope.launch {
            val flow = if (filterPlatform != null) {
                database.downloadHistoryDao().getHistoryByPlatform(filterPlatform)
            } else {
                database.downloadHistoryDao().getAllHistory()
            }
            
            flow.collectLatest { historyList ->
                if (historyList.isEmpty()) {
                    binding.emptyText.visibility = android.view.View.VISIBLE
                    binding.historyRecyclerView.visibility = android.view.View.GONE
                } else {
                    binding.emptyText.visibility = android.view.View.GONE
                    binding.historyRecyclerView.visibility = android.view.View.VISIBLE
                    historyAdapter.submitList(historyList)
                }
            }
        }
    }
    
    private fun reloadVideo(history: DownloadHistory) {
        if (history.status == "completed" && history.filePath != null) {
            // Check if file exists
            val file = File(history.filePath)
            if (file.exists()) {
                // Move to Downloads if not already there
                moveVideoToDownloads(file)
            } else {
                // File doesn't exist, re-download
                downloadVideo(history.url, history.platform)
            }
        } else {
            // Restart download
            downloadVideo(history.url, history.platform)
        }
    }
    
    private fun downloadVideo(url: String, platform: String = "tiktok") {
        lifecycleScope.launch {
            try {
                // Download video using appropriate downloader
                val filePath = withContext(Dispatchers.IO) {
                    tiktokDownloader.download(url)
                }
                
                val file = File(filePath)
                val fileName = file.name
                
                // Create or update history entry
                val newHistory = DownloadHistory(
                    taskId = fileName,
                    url = url,
                    fileName = fileName,
                    filePath = filePath,
                    platform = platform,
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
                database.downloadHistoryDao().insert(newHistory)
                
                // Move file to Downloads folder
                moveVideoToDownloads(file)
                Toast.makeText(this@HistoryActivity, "Download completed", Toast.LENGTH_SHORT).show()
                
            } catch (e: DownloadError) {
                Toast.makeText(this@HistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HistoryActivity", "Download error", e)
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HistoryActivity", "Unexpected error", e)
            }
        }
    }
    
    private fun moveVideoToDownloads(sourceFile: File) {
        if (!sourceFile.exists()) {
            Log.e("HistoryActivity", "Source file does not exist: ${sourceFile.absolutePath}")
            return
        }
        
        lifecycleScope.launch {
            try {
                val newFileName = generateTimestampFileName()
                val result = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveVideoWithMediaStoreFromFile(newFileName, sourceFile)
                    } else {
                        saveVideoDirectlyFromFile(newFileName, sourceFile)
                    }
                }
                
                if (result.success) {
                    sourceFile.delete()
                    Toast.makeText(
                        this@HistoryActivity,
                        "Video saved to Downloads/TikTokDownloads: $newFileName",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@HistoryActivity,
                        "Failed to save file: ${result.error}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("HistoryActivity", "Save failed: ${result.error}")
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Exception moving file", e)
                Toast.makeText(
                    this@HistoryActivity,
                    "Error saving file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun saveVideoDirectlyFromFile(fileName: String, sourceFile: File): SaveResult {
        return try {
            val finalFileName = if (!fileName.endsWith(".mp4", ignoreCase = true)) {
                fileName.replace(Regex("\\.[^.]+$"), "") + ".mp4"
            } else {
                fileName
            }
            
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return SaveResult(false, "External storage not available")
            }
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                return SaveResult(false, "Cannot create Downloads directory")
            }
            
            val tiktokDir = File(downloadsDir, "TikTokDownloads")
            if (!tiktokDir.exists() && !tiktokDir.mkdirs()) {
                return SaveResult(false, "Cannot create TikTokDownloads folder")
            }
            
            val destFile = File(tiktokDir, finalFileName)
            sourceFile.copyTo(destFile, overwrite = true)
            
            if (!destFile.exists() || destFile.length() == 0L) {
                return SaveResult(false, "File was not copied correctly")
            }
            
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(destFile)
            sendBroadcast(mediaScanIntent)
            
            SaveResult(true)
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error saving file directly", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun saveVideoWithMediaStoreFromFile(fileName: String, sourceFile: File): SaveResult {
        return try {
            val finalFileName = if (!fileName.endsWith(".mp4", ignoreCase = true)) {
                fileName.replace(Regex("\\.[^.]+$"), "") + ".mp4"
            } else {
                fileName
            }
            
            var contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TikTokDownloads")
            }
            
            val downloadsUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            var uri = contentResolver.insert(downloadsUri, contentValues)
            
            if (uri == null) {
                contentValues.remove(MediaStore.MediaColumns.RELATIVE_PATH)
                uri = contentResolver.insert(downloadsUri, contentValues)
            }
            
            if (uri == null) {
                return SaveResult(false, "MediaStore not available")
            }
            
            val outputStream = contentResolver.openOutputStream(uri)
                ?: return SaveResult(false, "Cannot open output stream")
            
            var bytesWritten = 0L
            outputStream.use { output ->
                sourceFile.inputStream().use { input ->
                    bytesWritten = input.copyTo(output)
                }
            }
            
            if (bytesWritten == 0L) {
                return SaveResult(false, "No data was written to file")
            }
            
            val updateValues = ContentValues().apply {
                put(MediaStore.MediaColumns.SIZE, bytesWritten)
            }
            contentResolver.update(uri, updateValues, null, null)
            
            SaveResult(true)
        } catch (e: SecurityException) {
            Log.e("HistoryActivity", "SecurityException saving file", e)
            SaveResult(false, "Permission denied")
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error saving file with MediaStore", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun generateTimestampFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        return "${dateFormat.format(Date())}.mp4"
    }
    
    data class SaveResult(val success: Boolean, val error: String? = null)
}


