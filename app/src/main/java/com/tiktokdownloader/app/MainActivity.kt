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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tiktokDownloader: PythonTikTokDownloader
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
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        
        // Initialize Python runtime for TikTok downloader
        PythonTikTokDownloader.initializePython(this)
        
        // Initialize Python TikTok downloader with cache directory
        val cacheDir = File(cacheDir, "tiktok_downloads")
        cacheDir.mkdirs()
        tiktokDownloader = PythonTikTokDownloader(cacheDir)
        
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
        binding.statusText.text = "Downloading video..."
        
        lifecycleScope.launch {
            try {
                // Download video directly using local TikTokDownloader
                val filePath = withContext(Dispatchers.IO) {
                    tiktokDownloader.download(url)
                }
                
                val file = File(filePath)
                val fileName = file.name
                
                binding.statusText.text = "Download completed!"
                
                // Save to database
                val history = DownloadHistory(
                    taskId = fileName,
                    url = url,
                    fileName = fileName,
                    filePath = filePath,
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
                database.downloadHistoryDao().insert(history)
                
                // Move file to Downloads folder
                moveVideoToDownloads(file)
                loadRecentDownloads()
                
            } catch (e: DownloadError) {
                binding.statusText.text = "Error: ${e.message}"
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("MainActivity", "Download error", e)
            } catch (e: Exception) {
                binding.statusText.text = "Error: ${e.message}"
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("MainActivity", "Unexpected error", e)
            } finally {
                binding.downloadButton.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun moveVideoToDownloads(sourceFile: File) {
        if (!sourceFile.exists()) {
            Log.e("MainActivity", "Source file does not exist: ${sourceFile.absolutePath}")
            return
        }
        
        lifecycleScope.launch {
            try {
                val newFileName = generateTimestampFileName()
                val result = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+: Use MediaStore
                        saveVideoWithMediaStoreFromFile(newFileName, sourceFile)
                    } else {
                        // Android 9 and below: Direct file access
                        saveVideoDirectlyFromFile(newFileName, sourceFile)
                    }
                }
                
                if (result.success) {
                    // Delete source file from cache
                    sourceFile.delete()
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
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception moving file", e)
                Toast.makeText(
                    this@MainActivity,
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
            Log.e("MainActivity", "Error saving file directly", e)
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
            Log.e("MainActivity", "SecurityException saving file", e)
            SaveResult(false, "Permission denied")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving file with MediaStore", e)
            SaveResult(false, e.message ?: "Error: ${e.javaClass.simpleName}")
        }
    }
    
    private fun generateTimestampFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        return "${dateFormat.format(Date())}.mp4"
    }
    
    data class SaveResult(val success: Boolean, val error: String? = null)
    
    
    private fun reloadVideo(history: DownloadHistory) {
        if (history.status == "completed" && history.filePath != null) {
            // Check if file exists
            val file = File(history.filePath)
            if (file.exists()) {
                // Move to Downloads if not already there
                moveVideoToDownloads(file)
            } else {
                // File doesn't exist, re-download
                downloadVideo(history.url)
            }
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


