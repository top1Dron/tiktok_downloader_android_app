package com.tiktokdownloader.app

import android.content.Context
import java.io.File

/**
 * Application configuration loaded from app.env file.
 * 
 * Create an app.env file in app/src/main/assets/ with:
 * BASE_URL=http://10.0.2.2:8000/
 * API_KEY=your-api-key-here
 * 
 * Note: We use app.env instead of .env because Android's build system
 * excludes files starting with '.' from assets by default.
 */
object AppConfig {
    private val envMap = mutableMapOf<String, String>()
    
    /**
     * Initialize configuration from .env file.
     * Should be called in Application.onCreate() or MainActivity.onCreate()
     */
    fun initialize(context: Context) {
        try {
            // First, list all assets to verify .env is there
            try {
                val assetList = context.assets.list("")
                android.util.Log.d("AppConfig", "Assets folder contents: ${assetList?.joinToString(", ") ?: "null"}")
                val hasEnv = assetList?.contains("app.env") == true
                android.util.Log.d("AppConfig", "app.env file found in assets: $hasEnv")
            } catch (e: Exception) {
                android.util.Log.w("AppConfig", "Could not list assets: ${e.message}")
            }
            
            // Load .env from assets folder
            // Note: context.assets automatically points to app/src/main/assets/ folder
            // Files in assets/ are bundled into APK at build time
            // No path needed - AssetManager knows where to find bundled assets
            android.util.Log.d("AppConfig", "Attempting to open app.env file from assets...")
            val envContent = context.assets.open("app.env").bufferedReader().use { it.readText() }
            android.util.Log.d("AppConfig", "Successfully loaded .env file from assets, content length: ${envContent.length}")
            
            // Log raw .env content for debugging
            android.util.Log.i("AppConfig", "📄 Raw .env file content (${envContent.length} chars):\n$envContent")
            
            // Parse .env file directly - simple and reliable
            envMap.clear()
            var parsedCount = 0
            envContent.lines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        if (key.isNotEmpty() && value.isNotEmpty()) {
                            envMap[key] = value
                            parsedCount++
                            android.util.Log.i("AppConfig", "✅ Parsed line ${index + 1}: $key = $value")
                        } else {
                            android.util.Log.w("AppConfig", "⚠️ Skipped empty key/value on line ${index + 1}: $trimmed")
                        }
                    } else {
                        android.util.Log.w("AppConfig", "⚠️ Skipped malformed line ${index + 1}: $trimmed")
                    }
                }
            }
            android.util.Log.i("AppConfig", "✅ Successfully parsed $parsedCount environment variables")
            android.util.Log.i("AppConfig", "✅ Environment map: $envMap")
            
            // Validate and log loaded values
            val baseUrl = envMap["BASE_URL"]?.trim() ?: "NOT FOUND"
            val apiKey = envMap["API_KEY"]?.trim() ?: ""
            android.util.Log.i("AppConfig", "📋 Loaded BASE_URL: '$baseUrl'")
            android.util.Log.i("AppConfig", "📋 Loaded API_KEY: '${if (apiKey.isNotEmpty()) "***${apiKey.takeLast(4)}" else "empty"}'")
            
            // Validate BASE_URL format
            if (baseUrl != "NOT FOUND" && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                android.util.Log.e("AppConfig", "❌ Invalid BASE_URL format: '$baseUrl'. Should start with http:// or https://")
            }
        } catch (e: java.io.FileNotFoundException) {
            // app.env file not found in assets
            android.util.Log.e("AppConfig", "❌ ERROR: app.env file not found in assets folder! Make sure app/src/main/assets/app.env exists and rebuild the APK.", e)
            envMap.clear()
        } catch (e: Exception) {
            // Other error loading .env file
            android.util.Log.e("AppConfig", "❌ ERROR: Could not load .env file: ${e.javaClass.simpleName}: ${e.message}. Using default values.", e)
            android.util.Log.e("AppConfig", "Stack trace:", e)
            envMap.clear()
        }
    }
    
    /**
     * Base URL for the API server.
     * Default: http://10.0.2.2:8000/ (for Android emulator)
     * For physical device: Use your computer's IP address
     */
    val BASE_URL: String
        get() {
            val url = envMap["BASE_URL"]?.trim() ?: "http://10.0.2.2:8000/"
            
            // Always log which URL is being used
            if (envMap.containsKey("BASE_URL")) {
                android.util.Log.i("AppConfig", "✅ Using BASE_URL from .env: $url")
            } else {
                android.util.Log.w("AppConfig", "❌ Using default BASE_URL: $url (not found in .env)")
                android.util.Log.w("AppConfig", "   Available env keys: ${envMap.keys.joinToString(", ")}")
            }
            return url
        }
    
    /**
     * API key for authentication.
     * Set this in .env file or leave empty if backend doesn't require API key.
     */
    val API_KEY: String
        get() = envMap["API_KEY"]?.trim() ?: ""
    
    /**
     * Get WebSocket URL from base URL.
     * Converts http:// to ws:// and https:// to wss://
     */
    fun getWebSocketUrl(): String {
        val baseUrl = BASE_URL.trimEnd('/')
        return when {
            baseUrl.startsWith("http://") -> baseUrl.replace("http://", "ws://")
            baseUrl.startsWith("https://") -> baseUrl.replace("https://", "wss://")
            else -> "ws://$baseUrl"
        }
    }
}

