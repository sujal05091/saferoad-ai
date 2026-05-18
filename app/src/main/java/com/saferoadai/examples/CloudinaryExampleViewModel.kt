package com.saferoadai.examples

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoadai.utils.CloudinaryManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Example ViewModel showing how to use Cloudinary for uploads
 */
class CloudinaryExampleViewModel : ViewModel() {
    
    /**
     * Example 1: Upload image from ride
     */
    fun uploadRidePhoto(context: Context, photoFile: File) {
        viewModelScope.launch {
            try {
                Toast.makeText(context, "Uploading photo...", Toast.LENGTH_SHORT).show()
                
                val photoUrl = CloudinaryManager.uploadFile(
                    file = photoFile,
                    folder = "saferoadai/rides/photos",
                    publicId = "ride_${System.currentTimeMillis()}"
                )
                
                Toast.makeText(context, "Photo uploaded!", Toast.LENGTH_SHORT).show()
                
                // Save URL to Firestore
                // saveRidePhotoToFirestore(photoUrl)
                
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Example 2: Upload video with progress
     */
    fun uploadRideVideo(context: Context, videoFile: File) {
        CloudinaryManager.uploadVideoWithProgress(
            file = videoFile,
            folder = "saferoadai/rides/videos",
            onProgress = { percentage ->
                // Update progress UI: percentage (0-100)
                println("Upload progress: $percentage%")
            },
            onSuccess = { videoUrl ->
                Toast.makeText(context, "Video uploaded!", Toast.LENGTH_SHORT).show()
                // Save URL to Firestore
                // saveRideVideoToFirestore(videoUrl)
            },
            onError = { error ->
                Toast.makeText(
                    context,
                    "Upload failed: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
    
    /**
     * Example 3: Upload detection snapshot
     */
    suspend fun uploadDetectionSnapshot(file: File): String {
        return CloudinaryManager.uploadFile(
            file = file,
            folder = "saferoadai/detections",
            publicId = "detection_${System.currentTimeMillis()}"
        )
    }
}
