package com.tiktokdownloader.app

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Configuration is loaded from .env file via AppConfig
// See app/src/main/assets/.env or create one from .env.example

data class DownloadRequest(val url: String)
data class DownloadResponse(
    val success: Boolean,
    val message: String,
    val task_id: String?,
    val file_path: String?,
    val file_name: String?
)

data class DownloadStatus(
    val task_id: String,
    val status: String,
    val url: String,
    val file_path: String?,
    val file_name: String?,
    val error: String?,
    val created_at: String,
    val completed_at: String?
)

interface ApiService {
    @POST("download")
    suspend fun downloadVideo(
        @Body request: DownloadRequest,
        @Header("X-API-Key") apiKey: String = AppConfig.API_KEY
    ): DownloadResponse
    
    @GET("download/status/{task_id}")
    suspend fun getDownloadStatus(
        @Path("task_id") taskId: String,
        @Header("X-API-Key") apiKey: String = AppConfig.API_KEY
    ): DownloadStatus
    
    @GET("download/file/{file_name}")
    suspend fun getVideoFile(
        @Path("file_name") fileName: String,
        @Header("X-API-Key") apiKey: String = AppConfig.API_KEY
    ): retrofit2.Response<ResponseBody>
    
    companion object {
        fun create(): ApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            // Add API key interceptor (only if API_KEY is set)
            val clientBuilder = OkHttpClient.Builder()
            
            if (AppConfig.API_KEY.isNotEmpty()) {
                val apiKeyInterceptor = Interceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("X-API-Key", AppConfig.API_KEY)
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                clientBuilder.addInterceptor(apiKeyInterceptor)
            }
            
            val client = clientBuilder
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}

