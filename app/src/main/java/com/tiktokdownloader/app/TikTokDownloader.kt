package com.tiktokdownloader.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Custom exception for download errors that should return user-friendly messages.
 */
class DownloadError(message: String) : Exception(message)

/**
 * TikTok video downloader that works directly on Android without backend.
 * Downloads TikTok videos by extracting video URLs and downloading them.
 */
class TikTokDownloader(private val outputDir: File) {
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    companion object {
        private const val TAG = "TikTokDownloader"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    
    /**
     * Download a TikTok video from the given URL.
     * 
     * @param url TikTok video URL
     * @return Path to the downloaded video file
     * @throws DownloadError if download fails
     */
    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download for URL: $url")
            
            // Validate URL
            if (!url.contains("tiktok.com")) {
                throw DownloadError("Invalid TikTok URL")
            }
            
            // Extract video ID from URL
            val videoId = extractVideoId(url)
            if (videoId == null) {
                throw DownloadError("Could not extract video ID from URL")
            }
            
            Log.d(TAG, "Extracted video ID: $videoId")
            
            // Get video info and download URL
            val videoUrl = getVideoDownloadUrl(url)
            if (videoUrl == null) {
                throw DownloadError("Video not available or could not extract download URL")
            }
            
            Log.d(TAG, "Found video URL, starting download...")
            
            // Download the video
            val fileName = "${videoId}.mp4"
            val outputFile = File(outputDir, fileName)
            
            downloadVideoFile(videoUrl, outputFile)
            
            if (!outputFile.exists() || outputFile.length() == 0L) {
                throw DownloadError("Downloaded file is empty or not found")
            }
            
            Log.d(TAG, "Download completed: ${outputFile.absolutePath}")
            outputFile.absolutePath
            
        } catch (e: DownloadError) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            throw DownloadError("Failed to download video: ${e.message}")
        }
    }
    
    /**
     * Extract video ID from TikTok URL.
     */
    private fun extractVideoId(url: String): String? {
        // Pattern for TikTok URLs like:
        // https://www.tiktok.com/@username/video/1234567890
        // https://www.tiktok.com/t/ZThJMSDvK/
        // https://vm.tiktok.com/ZThJMSDvK/
        
        val patterns = listOf(
            Pattern.compile("tiktok\\.com/.+?/video/(\\d+)"),
            Pattern.compile("tiktok\\.com/t/([A-Za-z0-9]+)"),
            Pattern.compile("vm\\.tiktok\\.com/([A-Za-z0-9]+)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        return null
    }
    
    /**
     * Get video download URL by parsing TikTok page.
     */
    private suspend fun getVideoDownloadUrl(pageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            // Normalize URL - ensure it's a full URL
            var normalizedUrl = pageUrl.trim()
            if (!normalizedUrl.startsWith("http")) {
                normalizedUrl = "https://$normalizedUrl"
            }
            
            // Create request with browser-like headers
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Cache-Control", "max-age=0")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch page: ${response.code}")
                return@withContext null
            }
            
            val html = response.body?.string() ?: return@withContext null
            Log.d(TAG, "Fetched HTML, length: ${html.length}")
            
            // Method 1: Try to find __UNIVERSAL_DATA_FOR_REHYDRATION__ script tag
            val universalDataPattern = Pattern.compile(
                "<script[^>]*id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>(.+?)</script>",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
            val universalMatcher = universalDataPattern.matcher(html)
            if (universalMatcher.find()) {
                try {
                    val jsonStr = universalMatcher.group(1).trim()
                    Log.d(TAG, "Found __UNIVERSAL_DATA_FOR_REHYDRATION__, length: ${jsonStr.length}")
                    val json = JSONObject(jsonStr)
                    val videoUrl = findVideoUrlInJson(json)
                    if (videoUrl != null) {
                        Log.d(TAG, "Found video URL in JSON: $videoUrl")
                        return@withContext videoUrl
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing __UNIVERSAL_DATA_FOR_REHYDRATION__ JSON", e)
                }
            }
            
            // Method 2: Try to find video URL patterns directly in HTML (unescaped)
            val patterns = listOf(
                "\"downloadAddr\"\\s*:\\s*\"([^\"]+)\"",
                "\"playAddr\"\\s*:\\s*\"([^\"]+)\"",
                "\"videoUrl\"\\s*:\\s*\"([^\"]+)\"",
                "\"download_url\"\\s*:\\s*\"([^\"]+)\"",
                "\"play_url\"\\s*:\\s*\"([^\"]+)\"",
                "downloadAddr\":\"([^\"]+)\"",
                "playAddr\":\"([^\"]+)\""
            )
            
            for (patternStr in patterns) {
                val pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    var videoUrl = matcher.group(1)
                    // Unescape URL
                    videoUrl = videoUrl.replace("\\u002F", "/")
                    videoUrl = videoUrl.replace("\\/", "/")
                    videoUrl = videoUrl.replace("\\\"", "\"")
                    videoUrl = videoUrl.replace("\\\\", "\\")
                    
                    // Check if it's a valid HTTP URL
                    if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                        Log.d(TAG, "Found video URL via pattern: $videoUrl")
                        return@withContext videoUrl
                    }
                }
            }
            
            // Method 3: Try to find JSON in window.__UNIVERSAL_DATA_FOR_REHYDRATION__
            try {
                // Use a simpler pattern that avoids regex quantifier issues
                val windowDataPattern = Pattern.compile(
                    "window\\.__UNIVERSAL_DATA_FOR_REHYDRATION__\\s*=\\s*(\\{[\\s\\S]*?\\});",
                    Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                )
                val windowMatcher = windowDataPattern.matcher(html)
                if (windowMatcher.find()) {
                    try {
                        val jsonStr = windowMatcher.group(1)
                        val json = JSONObject(jsonStr)
                        val videoUrl = findVideoUrlInJson(json)
                        if (videoUrl != null) {
                            Log.d(TAG, "Found video URL in window data: $videoUrl")
                            return@withContext videoUrl
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing window data JSON", e)
                    }
                }
            } catch (e: PatternSyntaxException) {
                Log.e(TAG, "Regex pattern error for window data", e)
                // Try alternative pattern without braces
                try {
                    val altPattern = Pattern.compile(
                        "window\\.__UNIVERSAL_DATA_FOR_REHYDRATION__\\s*=\\s*([^;]+);",
                        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                    )
                    val altMatcher = altPattern.matcher(html)
                    if (altMatcher.find()) {
                        var jsonStr = altMatcher.group(1).trim()
                        // Remove leading/trailing braces if present
                        if (jsonStr.startsWith("{")) jsonStr = jsonStr.substring(1)
                        if (jsonStr.endsWith("}")) jsonStr = jsonStr.substring(0, jsonStr.length - 1)
                        jsonStr = "{$jsonStr}"
                        val json = JSONObject(jsonStr)
                        val videoUrl = findVideoUrlInJson(json)
                        if (videoUrl != null) {
                            Log.d(TAG, "Found video URL in window data (alt method): $videoUrl")
                            return@withContext videoUrl
                        }
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Alternative pattern also failed", e2)
                }
            }
            
            // Method 4: Try to extract from SIGI_STATE
            val sigiPattern = Pattern.compile(
                "<script[^>]*id=\"SIGI_STATE\"[^>]*>(.+?)</script>",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
            val sigiMatcher = sigiPattern.matcher(html)
            if (sigiMatcher.find()) {
                try {
                    val jsonStr = sigiMatcher.group(1).trim()
                    val json = JSONObject(jsonStr)
                    val videoUrl = findVideoUrlInJson(json)
                    if (videoUrl != null) {
                        Log.d(TAG, "Found video URL in SIGI_STATE: $videoUrl")
                        return@withContext videoUrl
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SIGI_STATE JSON", e)
                }
            }
            
            // Log a sample of the HTML for debugging (first 2000 chars)
            val sample = html.take(2000)
            Log.w(TAG, "Could not find video URL. HTML sample: $sample")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video URL", e)
            null
        }
    }
    
    /**
     * Recursively search for video URL in JSON object.
     */
    private fun findVideoUrlInJson(json: Any?, depth: Int = 0): String? {
        // Limit recursion depth to avoid stack overflow
        if (depth > 10) {
            return null
        }
        
        when (json) {
            is JSONObject -> {
                // Check common keys first (most likely locations)
                val commonKeys = listOf(
                    "downloadAddr", "playAddr", "videoUrl", "url", "src",
                    "download_url", "play_url", "video_url", "videoUrlNoWaterMark",
                    "downloadAddrNoWaterMark", "playAddrNoWaterMark", "video",
                    "videoMeta", "itemInfo", "itemList", "videoData"
                )
                for (key in commonKeys) {
                    if (json.has(key)) {
                        val value = json.opt(key)
                        when (value) {
                            is String -> {
                                if (value.startsWith("http") && (value.contains(".mp4") || value.contains("video") || value.contains("cdn"))) {
                                    return value
                                }
                            }
                            is JSONObject -> {
                                val result = findVideoUrlInJson(value, depth + 1)
                                if (result != null) return result
                            }
                        }
                    }
                }
                
                // Recursively search in all values
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = json.opt(key)
                    val result = findVideoUrlInJson(value, depth + 1)
                    if (result != null) {
                        return result
                    }
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until json.length()) {
                    val result = findVideoUrlInJson(json.opt(i), depth + 1)
                    if (result != null) {
                        return result
                    }
                }
            }
            is String -> {
                // Sometimes the URL is directly in a string value
                if (json.startsWith("http") && (json.contains(".mp4") || json.contains("video") || json.contains("cdn"))) {
                    return json
                }
            }
        }
        return null
    }
    
    /**
     * Download video file from URL to local file.
     */
    private suspend fun downloadVideoFile(videoUrl: String, outputFile: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(videoUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.tiktok.com/")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw DownloadError("Failed to download video: HTTP ${response.code}")
        }
        
        val body = response.body ?: throw DownloadError("Response body is null")
        
        // Ensure output directory exists
        outputFile.parentFile?.mkdirs()
        
        // Write to file
        FileOutputStream(outputFile).use { output ->
            body.byteStream().use { input ->
                input.copyTo(output)
            }
        }
        
        Log.d(TAG, "Video file saved: ${outputFile.absolutePath}, size: ${outputFile.length()}")
    }
}

