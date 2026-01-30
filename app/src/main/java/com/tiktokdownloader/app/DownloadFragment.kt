package com.tiktokdownloader.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.tiktokdownloader.app.data.AppDatabase
import com.tiktokdownloader.app.data.DownloadHistory
import com.tiktokdownloader.app.databinding.FragmentDownloadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadFragment : Fragment() {
    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var database: AppDatabase
    private lateinit var recentAdapter: RecentDownloadAdapter
    private var platform: String = "tiktok"
    
    companion object {
        private const val ARG_PLATFORM = "platform"
        
        fun newInstance(platform: String): DownloadFragment {
            return DownloadFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLATFORM, platform)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        platform = arguments?.getString(ARG_PLATFORM) ?: "tiktok"
        database = AppDatabase.getDatabase(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }
        
        // Update subtitle based on platform
        binding.subtitleText.text = when (platform) {
            "instagram" -> "Enter Instagram URL to download"
            else -> "Enter TikTok URL to download"
        }
        
        // Setup RecyclerView
        recentAdapter = RecentDownloadAdapter { history ->
            reloadVideo(history)
        }
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerView.adapter = recentAdapter
        
        setupClickListeners()
        loadRecentDownloads()
    }
    
    private fun setupClickListeners() {
        // Paste from clipboard button
        binding.urlInputLayout.setEndIconOnClickListener {
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                binding.urlEditText.setText(text)
                Toast.makeText(requireContext(), "Pasted from clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.downloadButton.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val platformCheck = when (platform) {
                "instagram" -> "instagram.com"
                else -> "tiktok.com"
            }
            
            if (!url.contains(platformCheck)) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a valid ${platform.capitalize()} URL",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            downloadVideo(url)
        }
        
        binding.historyButton.setOnClickListener {
            val intent = Intent(requireContext(), HistoryActivity::class.java)
            intent.putExtra("platform", platform)
            startActivity(intent)
        }
    }
    
    private fun downloadVideo(url: String) {
        binding.downloadButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.statusText.text = "Starting download..."
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val python = Python.getInstance()
                    
                    // Select appropriate downloader module based on platform
                    val moduleName = when (platform) {
                        "instagram" -> "instagram_downloader"
                        else -> "tiktok_downloader"
                    }
                    val className = when (platform) {
                        "instagram" -> "InstagramDownloader"
                        else -> "TikTokDownloader"
                    }
                    
                    val module = python.getModule(moduleName)
                    val downloader = module.callAttr(className)
                    
                    // Use cache directory for temporary download
                    val cacheDir = File(requireContext().cacheDir, platform).apply { mkdirs() }
                    
                    // Set output directory
                    val pyOutputDir = python.builtins.callAttr("str", cacheDir.absolutePath)
                    downloader["output_dir"] = pyOutputDir
                    
                    // Download video
                    downloader.callAttr("download", url).toString()
                }
                
                val sourceFile = File(result)
                val originalTitle = sourceFile.nameWithoutExtension // Original TikTok/Instagram title
                
                // Move to public Downloads folder with timestamp name
                binding.statusText.text = "Saving to Downloads..."
                val timestampFileName = withContext(Dispatchers.IO) {
                    moveVideoToDownloads(sourceFile, platform)
                }
                
                // Save to database with original title for display
                val taskId = UUID.randomUUID().toString()
                val history = DownloadHistory(
                    taskId = taskId,
                    url = url,
                    fileName = originalTitle, // Keep original title for history display
                    filePath = timestampFileName, // Store actual timestamp filename
                    platform = platform,
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
                database.downloadHistoryDao().insert(history)
                
                binding.statusText.text = "Download completed!"
                Toast.makeText(
                    requireContext(),
                    "Video saved to Downloads/${platform.capitalize()}Downloads: $timestampFileName",
                    Toast.LENGTH_LONG
                ).show()
                
                // Clear input
                binding.urlEditText.text?.clear()
                
                // Reload recent downloads
                loadRecentDownloads()
                
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                binding.statusText.text = "Error: $errorMsg"
                Toast.makeText(
                    requireContext(),
                    "Error: $errorMsg",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.downloadButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun reloadVideo(history: DownloadHistory) {
        if (history.status == "completed" && history.fileName != null) {
            Toast.makeText(
                requireContext(),
                "Video already downloaded: ${history.fileName}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Re-download the video
            downloadVideo(history.url)
        }
    }
    
    private fun loadRecentDownloads() {
        lifecycleScope.launch {
            database.downloadHistoryDao().getRecentHistoryByPlatform(platform, 5).collectLatest { historyList ->
                recentAdapter.submitList(historyList)
            }
        }
    }
    
    private fun moveVideoToDownloads(sourceFile: File, platform: String): String {
        if (!sourceFile.exists()) {
            throw Exception("Source file does not exist")
        }
        
        // Generate timestamp filename
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss", java.util.Locale.getDefault())
        val timestamp = dateFormat.format(java.util.Date())
        val fileName = "$timestamp.mp4"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, 
                    "${android.os.Environment.DIRECTORY_DOWNLOADS}/${platform.capitalize()}Downloads")
            }
            
            val resolver = requireContext().contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")
            
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Failed to open output stream")
            
            // Delete source file from cache
            sourceFile.delete()
            
            return fileName
        } else {
            // Android 9 and below - Direct file access
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val platformDir = File(downloadsDir, "${platform.capitalize()}Downloads").apply { mkdirs() }
            val destFile = File(platformDir, fileName)
            
            sourceFile.copyTo(destFile, overwrite = true)
            sourceFile.delete()
            
            // Notify media scanner
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = android.net.Uri.fromFile(destFile)
            requireContext().sendBroadcast(mediaScanIntent)
            
            return fileName
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
