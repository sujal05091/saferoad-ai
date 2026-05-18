package com.saferoadai.detectors

import android.content.Context
import android.util.Log

/**
 * Traffic sign detection using TFLite model
 * TODO: Place traffic_signs.tflite in app/src/main/assets/models/
 */
class TrafficSignDetector(
    context: Context,
    modelPath: String = "models/traffic_signs.tflite.tflite",  // Fixed double extension
    inputSize: Int = 640,
    numClasses: Int = 5  // Model has 5 classes (9-4 bbox coords)
) : TFLiteDetector(context, modelPath, inputSize, numClasses = numClasses) {

    companion object {
        private const val TAG = "TrafficSignDetector"
        
        // ⚠️ UPDATE THESE to match your actual 5 trained classes!
        // Check your training notebook CLASS_NAMES list
        private val TRAFFIC_SIGN_LABELS = mapOf(
            0 to "Stop Sign",
            1 to "No Parking",
            2 to "No U-Turn",
            3 to "Speed Limit",
            4 to "Two Way Traffic",
            39 to "End No Overtaking (Trucks)"
        )
    }

    /**
     * Get traffic sign label from class ID
     */
    override fun getClassName(classId: Int): String {
        return TRAFFIC_SIGN_LABELS[classId] ?: "Unknown Sign ($classId)"
    }

    /**
     * Detect traffic signs with additional filtering
     */
    fun detectSigns(bitmap: android.graphics.Bitmap): List<SignDetectionResult> {
        Log.d(TAG, "🔍 TrafficSignDetector.detectSigns() called")
        val detections = detect(bitmap)
        Log.d(TAG, "📊 Base detect() returned ${detections.size} detections")
        
        return detections.map { detection ->
            SignDetectionResult(
                type = detection.className,
                confidence = detection.confidence,
                bbox = detection.bbox,
                classId = detection.classId,
                importance = calculateImportance(detection.classId)
            )
        }.sortedByDescending { it.importance }
    }

    /**
     * Calculate sign importance for prioritization
     * Critical signs (Stop, Yield, etc.) get higher priority
     */
    private fun calculateImportance(classId: Int): Int {
        return when (classId) {
            0 -> 10  // Stop Sign
            12 -> 10  // Yield
            14 -> 10  // No Entry
            13 -> 9   // Priority
            11 -> 8   // Priority Road
            15 -> 7   // General Caution
            else -> when {
                classId in 1..8 -> 6  // Speed limits
                classId in 16..28 -> 5  // Warning signs
                else -> 3  // Other signs
            }
        }
    }

    data class SignDetectionResult(
        val type: String,
        val confidence: Float,
        val bbox: android.graphics.RectF,
        val classId: Int,
        val importance: Int
    )
}
