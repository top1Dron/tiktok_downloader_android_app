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
    private lateinit var apiService: ApiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize configuration from .env file FIRST
        AppConfig.initialize(this)
        
        // Initialize API service AFTER AppConfig is loaded
        apiService = ApiService.create()
        
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
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
        lifecycleScope.launch {
            database.downloadHistoryDao().getAllHistory().collectLatest { historyList ->
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
        if (history.status == "completed" && history.fileName != null) {
            // Download the file again
            downloadVideoFile(history.fileName)
        } else {
            // Restart download
            lifecycleScope.launch {
                try {
                    val response = apiService.downloadVideo(DownloadRequest(history.url))
                    if (response.success && response.task_id != null) {
                        Toast.makeText(this@HistoryActivity, "Download restarted", Toast.LENGTH_SHORT).show()
                        // Update database
                        val updated = history.copy(
                            taskId = response.task_id,
                            status = "pending",
                            completedAt = null,
                            error = null
                        )
                        database.downloadHistoryDao().update(updated)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@HistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun downloadVideoFile(fileName: String) {
        lifecycleScope.launch {
            try {
                val encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                    .replace("+", "%20")
                val response = apiService.getVideoFile(encodedFileName)
                
                if (response.isSuccessful && response.body() != null) {
                    // Generate new filename with timestamp
                    val newFileName = generateTimestampFileName()
                    
                    val result = withContext(Dispatchers.IO) {
                        saveVideoToDownloads(newFileName, response.body()!!)
                    }
                    
                    if (result.success) {
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
                } else {
                    Toast.makeText(
                        this@HistoryActivity,
                        "Failed to download file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HistoryActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun generateTimestampFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        return "${dateFormat.format(Date())}.mp4"
    }
    
    data class SaveResult(val success: Boolean, val error: String? = null)
    
    private fun saveVideoToDownloads(fileName: String, body: okhttp3.ResponseBody): SaveResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Try MediaStore first (preferred)
                val downloadsResult = saveVideoWithMediaStore(fileName, body)
                if (downloadsResult.success) {
                    return downloadsResult
                }
                
                // If MediaStore fails, fallback to app directory (always works)
                Log.w("HistoryActivity", "MediaStore failed, saving to app directory")
                saveVideoToAppDirectory(fileName, body)
            } else {
                // Android 9 and below: Direct file access
                saveVideoDirectly(fileName, body)
            }
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Exception in saveVideoToDownloads", e)
            SaveResult(false, e.message ?: "Unknown error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun saveVideoToAppDirectory(fileName: String, body: okhttp3.ResponseBody): SaveResult {
        return try {
            // Ensure filename has .mp4 extension
            val finalFileName = if (!fileName.endsWith(".mp4", ignoreCase = true)) {
                fileName.replace(Regex("\\.[^.]+$"), "") + ".mp4"
            } else {
                fileName
            }
            
            // Save to app's external files directory as fallback
            val appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return SaveResult(false, "External storage not available")
            
            val tiktokDir = File(appDir, "TikTokDownloads")
            if (!tiktokDir.exists() && !tiktokDir.mkdirs()) {
                return SaveResult(false, "Cannot create directory")
            }
            
            val file = File(tiktokDir, finalFileName)
            
            // Use buffered streams and ensure proper flushing
            FileOutputStream(file).use { output ->
                val bufferedOutput = output.buffered(8192)
                body.byteStream().use { input ->
                    val bufferedInput = input.buffered(8192)
                    bufferedInput.copyTo(bufferedOutput)
                }
                bufferedOutput.flush()
                output.flush()
            }
            
            if (!file.exists() || file.length() == 0L) {
                return SaveResult(false, "File was not saved correctly")
            }
            
            // Notify media scanner
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            sendBroadcast(mediaScanIntent)
            
            Log.d("HistoryActivity", "File saved to app directory: ${file.absolutePath}, size: ${file.length()}")
            SaveResult(true, "Saved to app folder: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error saving to app directory", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun saveVideoDirectly(fileName: String, body: okhttp3.ResponseBody): SaveResult {
        return try {
            // Ensure filename has .mp4 extension
            val finalFileName = if (!fileName.endsWith(".mp4", ignoreCase = true)) {
                fileName.replace(Regex("\\.[^.]+$"), "") + ".mp4"
            } else {
                fileName
            }
            
            // Check if external storage is available
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return SaveResult(false, "External storage not available")
            }
            
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                return SaveResult(false, "Cannot create Downloads directory")
            }
            
            val tiktokDir = File(downloadsDir, "TikTokDownloads")
            
            // Create TikTokDownloads folder if it doesn't exist
            if (!tiktokDir.exists() && !tiktokDir.mkdirs()) {
                return SaveResult(false, "Cannot create TikTokDownloads folder")
            }
            
            if (!tiktokDir.canWrite()) {
                return SaveResult(false, "No write permission to TikTokDownloads folder")
            }
            
            val file = File(tiktokDir, finalFileName)
            
            // Use buffered streams and ensure proper flushing
            FileOutputStream(file).use { output ->
                val bufferedOutput = output.buffered(8192)
                body.byteStream().use { input ->
                    val bufferedInput = input.buffered(8192)
                    bufferedInput.copyTo(bufferedOutput)
                }
                bufferedOutput.flush()
                output.flush()
            }
            
            if (!file.exists() || file.length() == 0L) {
                return SaveResult(false, "File was not saved correctly")
            }
            
            // Notify media scanner
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            sendBroadcast(mediaScanIntent)
            
            Log.d("HistoryActivity", "File saved successfully: ${file.absolutePath}, size: ${file.length()}")
            SaveResult(true)
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error saving file directly", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun saveVideoWithMediaStore(fileName: String, body: okhttp3.ResponseBody): SaveResult {
        return try {
            // Ensure filename has .mp4 extension
            val finalFileName = if (!fileName.endsWith(".mp4", ignoreCase = true)) {
                fileName.replace(Regex("\\.[^.]+$"), "") + ".mp4"
            } else {
                fileName
            }
            
            // Save to Downloads/TikTokDownloads folder
            var contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                // Use RELATIVE_PATH to save to TikTokDownloads subfolder
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TikTokDownloads")
            }
            
            // Use MediaStore.Downloads for Android 10+ (API 29+)
            val downloadsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            
            var uri = contentResolver.insert(downloadsUri, contentValues)
            
            // If subfolder creation fails (some Android 10+ devices), try without subfolder
            if (uri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("HistoryActivity", "Subfolder creation failed, trying without subfolder")
                contentValues.remove(MediaStore.MediaColumns.RELATIVE_PATH)
                uri = contentResolver.insert(downloadsUri, contentValues)
            }
            
            // If Downloads collection fails, try Video collection
            if (uri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("HistoryActivity", "Downloads collection failed, trying Video collection with subfolder")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TikTokDownloads")
                uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            }
            
            if (uri == null) {
                return SaveResult(false, "MediaStore not available")
            }
            
            val outputStream = contentResolver.openOutputStream(uri)
                ?: return SaveResult(false, "Cannot open output stream")
            
            var bytesWritten = 0L
            outputStream.use { output ->
                // Use buffered streams for better performance and reliability
                val bufferedOutput = output.buffered(8192)
                body.byteStream().use { input ->
                    val bufferedInput = input.buffered(8192)
                    bytesWritten = bufferedInput.copyTo(bufferedOutput)
                    bufferedOutput.flush()
                }
            }
            
            if (bytesWritten == 0L) {
                return SaveResult(false, "No data was written to file")
            }
            
            // Update MediaStore with file size to ensure it's properly indexed
            val updateValues = ContentValues().apply {
                put(MediaStore.MediaColumns.SIZE, bytesWritten)
            }
            contentResolver.update(uri, updateValues, null, null)
            
            Log.d("HistoryActivity", "File saved successfully via MediaStore: $uri, bytes: $bytesWritten")
            SaveResult(true)
        } catch (e: SecurityException) {
            Log.e("HistoryActivity", "SecurityException saving file", e)
            SaveResult(false, "Permission denied")
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error saving file with MediaStore", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
}


