package com.tiktokdownloader.app

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * WebSocket service for receiving real-time download updates from the server.
 * 
 * Usage:
 * ```kotlin
 * val webSocketService = WebSocketService(AppConfig.getWebSocketUrl(), "android_client_1", AppConfig.API_KEY)
 * 
 * // Observe updates
 * lifecycleScope.launch {
 *     webSocketService.downloadUpdates.collect { update ->
 *         when (update.status) {
 *             "completed" -> // Handle completion
 *             "failed" -> // Handle error
 *             else -> // Handle other statuses
 *         }
 *     }
 * }
 * 
 * // Connect
 * webSocketService.connect()
 * 
 * // Disconnect when done
 * webSocketService.disconnect()
 * ```
 */
class WebSocketService(
    private val baseUrl: String,
    private val clientId: String,
    private val apiKey: String? = null
) {
    companion object {
        private const val TAG = "WebSocketService"
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS) // Keep connection alive
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false

    // StateFlow for download updates
    private val _downloadUpdates = MutableStateFlow<DownloadUpdate?>(null)
    val downloadUpdates: StateFlow<DownloadUpdate?> = _downloadUpdates.asStateFlow()

    // StateFlow for connection status
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    data class DownloadUpdate(
        val taskId: String,
        val status: String,
        val fileName: String?,
        val filePath: String?,
        val url: String?,
        val error: String?
    )

    enum class ConnectionStatus {
        Connecting,
        Connected,
        Disconnected,
        Error
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            isConnected = true
            _connectionStatus.value = ConnectionStatus.Connected
            
            // Send initial ping
            sendPing()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            
            try {
                val json = JSONObject(text)
                
                // Check if it's a pong response
                if (json.has("type") && json.getString("type") == "pong") {
                    Log.d(TAG, "Received pong from server")
                    return
                }
                
                // Parse download update
                val update = DownloadUpdate(
                    taskId = json.getString("task_id"),
                    status = json.getString("status"),
                    fileName = json.optString("file_name", null),
                    filePath = json.optString("file_path", null),
                    url = json.optString("url", null),
                    error = json.optString("error", null)
                )
                
                _downloadUpdates.value = update
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}", e)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            isConnected = false
            _connectionStatus.value = ConnectionStatus.Disconnected
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}", t)
            isConnected = false
            _connectionStatus.value = ConnectionStatus.Error
        }
    }

    /**
     * Connect to the WebSocket server.
     */
    fun connect() {
        if (isConnected) {
            Log.w(TAG, "Already connected")
            return
        }

        // Convert http/https to ws/wss
        val wsUrl = when {
            baseUrl.startsWith("http://") -> baseUrl.replace("http://", "ws://")
            baseUrl.startsWith("https://") -> baseUrl.replace("https://", "wss://")
            baseUrl.startsWith("ws://") || baseUrl.startsWith("wss://") -> baseUrl
            else -> "ws://$baseUrl"
        }

        val url = "$wsUrl/ws/$clientId"
        Log.d(TAG, "Connecting to: $url")

        val requestBuilder = Request.Builder().url(url)
        
        // Add API key header if provided
        if (apiKey != null) {
            requestBuilder.addHeader("X-API-Key", apiKey)
        }

        _connectionStatus.value = ConnectionStatus.Connecting
        webSocket = client.newWebSocket(requestBuilder.build(), listener)
    }

    /**
     * Disconnect from the WebSocket server.
     */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        isConnected = false
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    /**
     * Send a ping message to keep the connection alive.
     */
    private fun sendPing() {
        val message = JSONObject().apply {
            put("type", "ping")
        }.toString()
        
        webSocket?.send(message)
    }

    /**
     * Send a custom message to the server.
     */
    fun sendMessage(message: String): Boolean {
        return if (isConnected) {
            webSocket?.send(message) ?: false
        } else {
            Log.w(TAG, "Cannot send message: not connected")
            false
        }
    }

    /**
     * Reconnect to the WebSocket server.
     */
    fun reconnect() {
        disconnect()
        connect()
    }
}

