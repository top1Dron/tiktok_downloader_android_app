package com.tiktokdownloader.app

import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper for Python TikTokDownloader using Chaquopy.
 * This allows us to use the proven yt-dlp-based Python downloader in Android.
 */
class PythonTikTokDownloader(private val outputDir: File) {
    
    companion object {
        private const val TAG = "PythonTikTokDownloader"
        private var pythonInitialized = false
        
        /**
         * Initialize Python runtime (only needs to be done once).
         */
        fun initializePython(context: android.content.Context) {
            if (!pythonInitialized) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context))
                }
                pythonInitialized = true
                Log.d(TAG, "Python runtime initialized")
            }
        }
    }
    
    /**
     * Download a TikTok video from the given URL using Python yt-dlp.
     * 
     * @param url TikTok video URL
     * @return Path to the downloaded video file
     * @throws DownloadError if download fails
     */
    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Python download for URL: $url")
            
            val python = Python.getInstance()
            val tiktokModule = python.getModule("tiktok_downloader")
            val downloaderClass = tiktokModule["TikTokDownloader"]
                ?: throw DownloadError("TikTokDownloader class not found in Python module")
            
            // Create downloader instance with output directory
            val downloader = downloaderClass.call(outputDir.absolutePath)
                ?: throw DownloadError("Failed to create TikTokDownloader instance")
            
            // Call download method
            val result = downloader.callAttr("download", url)
                ?: throw DownloadError("Download method returned null")
            val filePath = result.toString()
            
            Log.d(TAG, "Python download completed: $filePath")
            filePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Python download error", e)
            val errorMessage = e.message ?: "Unknown error"
            
            // Check if it's a DownloadError from Python
            if (errorMessage.contains("Video not available") || 
                errorMessage.contains("Failed to download") ||
                errorMessage.contains("Download error")) {
                throw DownloadError(errorMessage)
            }
            
            throw DownloadError("Failed to download video: $errorMessage")
        }
    }
}

