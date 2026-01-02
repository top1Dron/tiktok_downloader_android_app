package com.tiktokdownloader.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tiktokdownloader.app.data.AppDatabase
import com.tiktokdownloader.app.data.DownloadHistory
import com.tiktokdownloader.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: ApiService
    private lateinit var database: AppDatabase
    private lateinit var recentAdapter: RecentDownloadAdapter
    
    // For Android 13+ (API 33+)
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_MEDIA_VIDEO
    )
    
    // For older Android versions
    private val legacyPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize configuration from .env file FIRST
        AppConfig.initialize(this)
        
        // Log the final BASE_URL being used (for immediate debugging)
        android.util.Log.i("MainActivity", "🔍 AppConfig.BASE_URL = ${AppConfig.BASE_URL}")
        
        // Initialize API service AFTER AppConfig is loaded
        apiService = ApiService.create()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Initialize database
        database = AppDatabase.getDatabase(this)
        
        // Setup RecyclerView
        recentAdapter = RecentDownloadAdapter { history ->
            reloadVideo(history)
        }
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentRecyclerView.adapter = recentAdapter
        
        setupClickListeners()
        requestPermissions()
        loadRecentDownloads()
    }
    
    private fun setupClickListeners() {
        // Initialize icon state based on current text
        updateEndIcon()
        
        // Show/hide clear button based on text content
        binding.urlEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateEndIcon()
            }
        })
        
        binding.downloadButton.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a TikTok URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!url.contains("tiktok.com")) {
                Toast.makeText(this, "Please enter a valid TikTok URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            downloadVideo(url)
        }
        
        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
    
    private fun updateEndIcon() {
        val hasText = binding.urlEditText.text?.isNotEmpty() == true
        
        if (hasText) {
            // Show clear button when there's text
            binding.urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CLEAR_TEXT
        } else {
            // Show paste button when empty
            binding.urlInputLayout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
            binding.urlInputLayout.setEndIconDrawable(com.tiktokdownloader.app.R.drawable.ic_paste)
            binding.urlInputLayout.setEndIconOnClickListener {
                pasteFromClipboard()
            }
        }
    }
    
    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        
        if (clipData != null && clipData.itemCount > 0) {
            val item = clipData.getItemAt(0)
            val text = item.text?.toString()?.trim()
            
            if (text != null && text.isNotEmpty()) {
                binding.urlEditText.setText(text)
                binding.urlEditText.setSelection(text.length) // Move cursor to end
                Toast.makeText(this, "URL pasted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadVideo(url: String) {
        binding.downloadButton.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.statusText.text = "Starting download..."
        
        lifecycleScope.launch {
            try {
                val response = apiService.downloadVideo(DownloadRequest(url))
                
                if (response.success && response.task_id != null) {
                    binding.statusText.text = "Download started"
                    
                    // Save to database
                    val history = DownloadHistory(
                        taskId = response.task_id,
                        url = url,
                        fileName = null,
                        filePath = null,
                        status = "pending"
                    )
                    database.downloadHistoryDao().insert(history)
                    
                    // Poll for status updates
                    pollDownloadStatus(response.task_id, url)
                } else {
                    binding.statusText.text = "Download failed: ${response.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "Download failed: ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.statusText.text = "Error: ${e.message}"
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.downloadButton.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun pollDownloadStatus(taskId: String, url: String) {
        lifecycleScope.launch {
            var attempts = 0
            val maxAttempts = 60 // Poll for up to 5 minutes (60 * 5 seconds)
            
            while (attempts < maxAttempts) {
                try {
                    kotlinx.coroutines.delay(5000) // Wait 5 seconds
                    val status = apiService.getDownloadStatus(taskId)
                    
                    // Update database
                    val history = database.downloadHistoryDao().getByTaskId(taskId)
                    val completedAt = if (status.completed_at != null) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                .parse(status.completed_at)?.time
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    } else null
                    
                    if (history != null) {
                        val updated = history.copy(
                            status = status.status,
                            fileName = status.file_name,
                            filePath = status.file_path,
                            error = status.error,
                            completedAt = completedAt
                        )
                        database.downloadHistoryDao().update(updated)
                    } else {
                        // Create new entry
                        val newHistory = DownloadHistory(
                            taskId = taskId,
                            url = url,
                            fileName = status.file_name,
                            filePath = status.file_path,
                            status = status.status,
                            error = status.error,
                            completedAt = completedAt
                        )
                        database.downloadHistoryDao().insert(newHistory)
                    }
                    
                    // Update UI
                    binding.statusText.text = "Status: ${status.status}"
                    
                    if (status.status == "completed") {
                        binding.statusText.text = "Download completed!"
                        downloadVideoFile(status.file_name ?: "")
                        loadRecentDownloads()
                        break
                    } else if (status.status == "failed") {
                        binding.statusText.text = "Download failed: ${status.error}"
                        break
                    }
                    
                    attempts++
                } catch (e: Exception) {
                    binding.statusText.text = "Error checking status: ${e.message}"
                    attempts++
                }
            }
        }
    }
    
    private fun downloadVideoFile(fileName: String) {
        if (fileName.isEmpty()) return
        
        // Check permissions before attempting to save
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // For Android 9 and below, need WRITE_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Storage permission required. Please grant permission and try again.",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissions()
                return
            }
        }
        
        lifecycleScope.launch {
            try {
                // URL encode the filename to handle special characters
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
                            this@MainActivity,
                            "Video saved to Downloads/TikTokDownloads: $newFileName",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to save file: ${result.error}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("MainActivity", "Save failed: ${result.error}")
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to download file: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("MainActivity", "Download failed: ${response.code()}")
                }
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                Toast.makeText(
                    this@MainActivity,
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
                Log.e("MainActivity", "Exception saving file", e)
            }
        }
    }
    
    private fun generateTimestampFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        return "${dateFormat.format(Date())}.mp4"
    }
    
    data class SaveResult(val success: Boolean, val error: String? = null)
    
    private fun saveVideoToDownloads(fileName: String, body: ResponseBody): SaveResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Try MediaStore first (preferred)
                val downloadsResult = saveVideoWithMediaStore(fileName, body)
                if (downloadsResult.success) {
                    return downloadsResult
                }
                
                // If MediaStore fails, fallback to app directory (always works)
                Log.w("MainActivity", "MediaStore failed, saving to app directory")
                saveVideoToAppDirectory(fileName, body)
            } else {
                // Android 9 and below: Direct file access
                saveVideoDirectly(fileName, body)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception in saveVideoToDownloads", e)
            SaveResult(false, e.message ?: "Unknown error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun saveVideoToAppDirectory(fileName: String, body: ResponseBody): SaveResult {
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
            
            Log.d("MainActivity", "File saved to app directory: ${file.absolutePath}, size: ${file.length()}")
            SaveResult(true, "Saved to app folder: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving to app directory", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    @Suppress("DEPRECATION")
    private fun saveVideoDirectly(fileName: String, body: ResponseBody): SaveResult {
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
            
            Log.d("MainActivity", "File saved successfully: ${file.absolutePath}, size: ${file.length()}")
            SaveResult(true)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving file directly", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun saveVideoWithMediaStore(fileName: String, body: ResponseBody): SaveResult {
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
                Log.d("MainActivity", "Subfolder creation failed, trying without subfolder")
                contentValues.remove(MediaStore.MediaColumns.RELATIVE_PATH)
                uri = contentResolver.insert(downloadsUri, contentValues)
            }
            
            // If Downloads collection fails, try Video collection
            if (uri == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("MainActivity", "Downloads collection failed, trying Video collection with subfolder")
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
            
            Log.d("MainActivity", "File saved successfully via MediaStore: $uri, bytes: $bytesWritten")
            SaveResult(true)
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException saving file", e)
            SaveResult(false, "Permission denied")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving file with MediaStore", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun reloadVideo(history: DownloadHistory) {
        if (history.status == "completed" && history.fileName != null) {
            downloadVideoFile(history.fileName)
        } else {
            // Restart download
            downloadVideo(history.url)
        }
    }
    
    private fun loadRecentDownloads() {
        lifecycleScope.launch {
            database.downloadHistoryDao().getRecentHistory(5).collectLatest { historyList ->
                recentAdapter.submitList(historyList)
            }
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        } else {
            legacyPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}

