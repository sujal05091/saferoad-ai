package com.saferoadai.detectors

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Roboflow API-based pothole detection
 * More accurate than local TFLite model
 */
class RoboflowDetector {
    
    companion object {
        private const val TAG = "RoboflowDetector"
        private const val API_URL = "https://detect.roboflow.com"
        private const val API_KEY = "wIKONKl0vQde5wH0eXAY"
        private const val MODEL_ID = "pothole-detection-i00zy/2"
        private const val CONFIDENCE_THRESHOLD = 0.40f  // 40% - much higher than weak model
    }
    
    data class Detection(
        val className: String,
        val confidence: Float,
        val bbox: RectF,
        val width: Int,
        val height: Int
    )
    
    /**
     * Detect potholes using Roboflow API
     * Requires internet connection
     */
    suspend fun detectPotholes(bitmap: Bitmap): List<Detection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🌐 Sending image to Roboflow API...")
            
            // Convert bitmap to base64 JPEG
            val base64Image = bitmapToBase64(bitmap)
            
            // Build API URL with parameters
            val urlString = "$API_URL/$MODEL_ID?api_key=$API_KEY&confidence=$CONFIDENCE_THRESHOLD"
            val url = URL(urlString)
            
            // Create HTTP connection
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            connection.connectTimeout = 10000  // 10 seconds
            connection.readTimeout = 10000
            
            // Send base64 image
            connection.outputStream.use { os ->
                os.write(base64Image.toByteArray())
            }
            
            // Read response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "✅ Roboflow response received")
                
                // Parse JSON response
                val detections = parseRoboflowResponse(response, bitmap.width, bitmap.height)
                Log.i(TAG, "📊 Roboflow detected ${detections.size} potholes")
                
                detections
            } else {
                Log.e(TAG, "❌ Roboflow API error: $responseCode")
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Error details: $errorStream")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Roboflow detection failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Convert bitmap to base64 encoded JPEG string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Parse Roboflow JSON response
     * Format: { "predictions": [ { "x": 100, "y": 100, "width": 50, "height": 50, "confidence": 0.95, "class": "pothole" } ] }
     */
    private fun parseRoboflowResponse(jsonResponse: String, imageWidth: Int, imageHeight: Int): List<Detection> {
        try {
            val json = JSONObject(jsonResponse)
            val predictions = json.optJSONArray("predictions") ?: return emptyList()
            
            val detections = mutableListOf<Detection>()
            
            for (i in 0 until predictions.length()) {
                val pred = predictions.getJSONObject(i)
                
                val className = pred.optString("class", "pothole")
                val confidence = pred.optDouble("confidence", 0.0).toFloat()
                
                // Roboflow returns center coordinates + width/height
                val centerX = pred.optDouble("x", 0.0).toFloat()
                val centerY = pred.optDouble("y", 0.0).toFloat()
                val width = pred.optDouble("width", 0.0).toFloat()
                val height = pred.optDouble("height", 0.0).toFloat()
                
                // Convert to corner coordinates
                val left = centerX - (width / 2f)
                val top = centerY - (height / 2f)
                val right = centerX + (width / 2f)
                val bottom = centerY + (height / 2f)
                
                val bbox = RectF(left, top, right, bottom)
                
                detections.add(Detection(
                    className = className,
                    confidence = confidence,
                    bbox = bbox,
                    width = imageWidth,
                    height = imageHeight
                ))
                
                Log.d(TAG, "  📍 $className: ${(confidence * 100).toInt()}% at ($centerX, $centerY)")
            }
            
            return detections
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Roboflow response: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Check if detection should trigger alert
     */
    fun shouldAlert(detection: Detection): Boolean {
        // Roboflow is more accurate, so we can use higher threshold
        return detection.confidence > 0.50f  // 50% confidence
    }
}
