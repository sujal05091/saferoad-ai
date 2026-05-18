package com.saferoadai.detectors

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * Road hazard detection (potholes, debris, cracks, etc.) using TFLite model
 * TODO: Place patholes.tflite in app/src/main/assets/models/
 */
class RoadHazardDetector(
    context: Context,
    modelPath: String = "models/patholes.tflite",
    inputSize: Int = 640,  // Model expects 640x640 input
    numClasses: Int = 1    // Model has only 1 class (pothole)
) : TFLiteDetector(context, modelPath, inputSize, numClasses = numClasses) {

    companion object {
        private const val TAG = "RoadHazardDetector"
        
        // Road hazard classes (adjust based on your trained model)
        private val HAZARD_LABELS = mapOf(
            0 to "Pothole",
            1 to "Crack",
            2 to "Debris",
            3 to "Road Damage",
            4 to "Construction Zone",
            5 to "Obstruction",
            6 to "Water Puddle",
            7 to "Manhole",
            8 to "Speed Bump",
            9 to "Uneven Surface"
        )
    }

    enum class HazardSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    data class HazardDetectionResult(
        val type: String,
        val confidence: Float,
        val bbox: android.graphics.RectF,
        val classId: Int,
        val severity: HazardSeverity
    )

    /**
     * Get hazard label from class ID
     */
    override fun getClassName(classId: Int): String {
        return HAZARD_LABELS[classId] ?: "Unknown Hazard ($classId)"
    }

    /**
     * Detect road hazards with severity assessment
     */
    fun detectHazards(bitmap: Bitmap): List<HazardDetectionResult> {
        Log.d(TAG, "🔍 RoadHazardDetector.detectHazards() called")
        val detections = detect(bitmap)
        Log.d(TAG, "📊 Base detect() returned ${detections.size} detections")
        
        return detections.map { detection ->
            HazardDetectionResult(
                type = detection.className,
                confidence = detection.confidence,
                bbox = detection.bbox,
                classId = detection.classId,
                severity = assessSeverity(detection.classId, detection.confidence, detection.bbox)
            )
        }.sortedByDescending { it.severity }
    }

    /**
     * Assess hazard severity based on type, confidence, and size
     */
    private fun assessSeverity(classId: Int, confidence: Float, bbox: android.graphics.RectF): HazardSeverity {
        val size = (bbox.width() * bbox.height())
        
        // Base severity on hazard type
        val baseSeverity = when (classId) {
            0 -> HazardSeverity.HIGH      // Pothole
            1 -> HazardSeverity.MEDIUM    // Crack
            2 -> HazardSeverity.HIGH      // Debris
            3 -> HazardSeverity.HIGH      // Road Damage
            4 -> HazardSeverity.MEDIUM    // Construction Zone
            5 -> HazardSeverity.CRITICAL  // Obstruction
            6 -> HazardSeverity.LOW       // Water Puddle
            7 -> HazardSeverity.MEDIUM    // Manhole
            8 -> HazardSeverity.LOW       // Speed Bump
            9 -> HazardSeverity.MEDIUM    // Uneven Surface
            else -> HazardSeverity.MEDIUM
        }
        
        // Adjust based on confidence and size
        return when {
            confidence > 0.8f && size > 10000 -> {
                // Large, high-confidence hazards are more severe
                when (baseSeverity) {
                    HazardSeverity.LOW -> HazardSeverity.MEDIUM
                    HazardSeverity.MEDIUM -> HazardSeverity.HIGH
                    HazardSeverity.HIGH -> HazardSeverity.CRITICAL
                    else -> baseSeverity
                }
            }
            confidence < 0.5f || size < 1000 -> {
                // Small or low-confidence hazards are less severe
                when (baseSeverity) {
                    HazardSeverity.CRITICAL -> HazardSeverity.HIGH
                    HazardSeverity.HIGH -> HazardSeverity.MEDIUM
                    HazardSeverity.MEDIUM -> HazardSeverity.LOW
                    else -> baseSeverity
                }
            }
            else -> baseSeverity
        }
    }

    /**
     * Check if hazard should trigger immediate alert
     * Adjusted for weak model (1-2% confidence range)
     */
    fun shouldAlert(hazard: HazardDetectionResult): Boolean {
        // For weak models, alert on any detection above 3% threshold
        return hazard.confidence > 0.03f  // Alert if detected (>3%)
    }
}
