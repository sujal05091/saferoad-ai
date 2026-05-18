package com.saferoadai.utils

import android.content.Context
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryManager {
    
    private var isInitialized = false
    
    /**
     * Initialize Cloudinary with your credentials
     * Call this in Application.onCreate() or before first use
     */
    fun init(context: Context) {
        if (isInitialized) return
        
        val config = mapOf(
            "cloud_name" to "dycudtwkj",  // Replace with your Cloud Name
            "api_key" to "554218888561629",        // Replace with your API Key
            "api_secret" to "pJcDA2ayTvbUm9hg8zAMO5bN8lE"   // Replace with your API Secret
        )
        
        MediaManager.init(context, config)
        isInitialized = true
    }
    
    /**
     * Upload file to Cloudinary
     * @param file File to upload (image or video)
     * @param folder Cloudinary folder path (e.g., "rides/images")
     * @param publicId Optional custom filename
     * @return Cloudinary URL of uploaded file
     */
    suspend fun uploadFile(
        file: File,
        folder: String = "saferoadai",
        publicId: String? = null
    ): String = suspendCancellableCoroutine { continuation ->
        
        val requestId = MediaManager.get().upload(file.absolutePath)
            .option("folder", folder)
            .apply {
                if (publicId != null) {
                    option("public_id", publicId)
                }
            }
            .option("resource_type", "auto") // Auto-detect image/video
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Upload started
                }
                
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Progress update: bytes/totalBytes
                }
                
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    if (url != null) {
                        continuation.resume(url)
                    } else {
                        continuation.resumeWithException(
                            Exception("Upload succeeded but no URL returned")
                        )
                    }
                }
                
                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(
                        Exception("Upload failed: ${error.description}")
                    )
                }
                
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Upload rescheduled due to network issue
                }
            })
            .dispatch()
        
        continuation.invokeOnCancellation {
            MediaManager.get().cancelRequest(requestId)
        }
    }
    
    /**
     * Upload video with progress callback
     */
    fun uploadVideoWithProgress(
        file: File,
        folder: String = "saferoadai/videos",
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(file.absolutePath)
            .option("folder", folder)
            .option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    onProgress(0)
                }
                
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val percentage = ((bytes * 100) / totalBytes).toInt()
                    onProgress(percentage)
                }
                
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    if (url != null) {
                        onSuccess(url)
                    } else {
                        onError("No URL returned")
                    }
                }
                
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description ?: "Upload failed")
                }
                
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Will retry automatically
                }
            })
            .dispatch()
    }
    
    /**
     * Delete file from Cloudinary
     * @param publicId The public ID of the file (from URL)
     */
    suspend fun deleteFile(publicId: String) {
        // Note: Deletion requires admin API key (server-side)
        // For security, implement deletion via your backend
    }
}
