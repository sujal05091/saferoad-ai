package com.saferoadai.face

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Face monitoring analyzer for drowsiness detection
 * Uses ML Kit Face Detection to calculate:
 * - EAR (Eye Aspect Ratio) for eye closure detection
 * - MAR (Mouth Aspect Ratio) for yawning detection
 * - Head pose (pitch, yaw, roll) for distraction detection
 * 
 * TODO: Can be replaced with MediaPipe FaceMesh for more accurate landmarks
 */
class FaceMonitorAnalyzer(
    private val context: Context,
    private val onDrowsinessDetected: (DrowsinessLevel, DrowsinessDetails) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceMonitorAnalyzer"
        
        // Thresholds (tune these based on testing)
        private const val EAR_THRESHOLD = 0.21f  // Eye closure threshold
        private const val MAR_THRESHOLD = 0.6f   // Yawning threshold
        private const val EAR_CONSECUTIVE_FRAMES = 15  // Frames before drowsy alert
        private const val YAWN_FRAMES = 10
        private const val HEAD_POSE_THRESHOLD = 25f  // Degrees
    }

    enum class DrowsinessLevel {
        ALERT,      // Normal, eyes open
        DROWSY,     // Eyes closing frequently
        VERY_DROWSY, // Eyes closed for extended period
        YAWNING,    // Mouth open (yawning)
        DISTRACTED  // Head turned away
    }

    data class DrowsinessDetails(
        val level: DrowsinessLevel,
        val earValue: Float,
        val marValue: Float,
        val headPoseYaw: Float,
        val headPosePitch: Float,
        val frameCount: Int,
        val message: String
    )

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    )

    private var consecutiveClosedEyes = 0
    private var consecutiveYawns = 0
    private var lastAlertTime = 0L
    private val alertCooldown = 3000L  // 3 seconds between alerts

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    // No face detected
                    consecutiveClosedEyes = 0
                    consecutiveYawns = 0
                } else {
                    processFaces(faces)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFaces(faces: List<Face>) {
        // Process first face only (driver)
        val face = faces.firstOrNull() ?: return

        // Calculate EAR (Eye Aspect Ratio)
        val ear = calculateEAR(face)
        
        // Calculate MAR (Mouth Aspect Ratio)
        val mar = calculateMAR(face)
        
        // Get head pose
        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ

        // Determine drowsiness state
        val drowsinessState = determineDrowsinessState(ear, mar, yaw, pitch)
        
        // Trigger callback if alert needed
        if (drowsinessState.level != DrowsinessLevel.ALERT) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAlertTime > alertCooldown) {
                onDrowsinessDetected(drowsinessState.level, drowsinessState)
                lastAlertTime = currentTime
            }
        }
    }

    /**
     * Calculate Eye Aspect Ratio (EAR)
     * EAR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)
     * 
     * Note: ML Kit doesn't provide all eye landmarks, so we use approximation
     * TODO: Use MediaPipe FaceMesh for accurate eye landmarks
     */
    private fun calculateEAR(face: Face): Float {
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        
        if (leftEye == null || rightEye == null) {
            return 1.0f  // Default: eyes open
        }

        // ML Kit provides eye open probability - use this as approximation
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
        
        // Convert probability to EAR-like metric
        // Higher probability = larger EAR (eyes open)
        return (leftEyeOpenProb + rightEyeOpenProb) / 2f * 0.3f
    }

    /**
     * Calculate Mouth Aspect Ratio (MAR)
     * MAR = ||p2 - p8|| / ||p1 - p4||
     */
    private fun calculateMAR(face: Face): Float {
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
        
        if (mouthLeft == null || mouthRight == null || mouthBottom == null) {
            return 0.0f  // Default: mouth closed
        }

        // Calculate distances
        val horizontalDist = distance(mouthLeft.position, mouthRight.position)
        val verticalDist = distance(
            PointF((mouthLeft.position.x + mouthRight.position.x) / 2, 
                   (mouthLeft.position.y + mouthRight.position.y) / 2),
            mouthBottom.position
        )
        
        return if (horizontalDist > 0) verticalDist / horizontalDist else 0f
    }

    /**
     * Calculate Euclidean distance between two points
     */
    private fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    /**
     * Determine drowsiness state from metrics
     */
    private fun determineDrowsinessState(
        ear: Float,
        mar: Float,
        yaw: Float,
        pitch: Float
    ): DrowsinessDetails {
        // Check for closed eyes
        if (ear < EAR_THRESHOLD) {
            consecutiveClosedEyes++
        } else {
            consecutiveClosedEyes = 0
        }

        // Check for yawning
        if (mar > MAR_THRESHOLD) {
            consecutiveYawns++
        } else {
            consecutiveYawns = 0
        }

        // Check head pose (driver distracted?)
        val isHeadTurned = abs(yaw) > HEAD_POSE_THRESHOLD || abs(pitch) > HEAD_POSE_THRESHOLD

        // Determine level and message
        return when {
            isHeadTurned -> DrowsinessDetails(
                level = DrowsinessLevel.DISTRACTED,
                earValue = ear,
                marValue = mar,
                headPoseYaw = yaw,
                headPosePitch = pitch,
                frameCount = 0,
                message = "Please keep your eyes on the road"
            )
            consecutiveClosedEyes > EAR_CONSECUTIVE_FRAMES -> DrowsinessDetails(
                level = DrowsinessLevel.VERY_DROWSY,
                earValue = ear,
                marValue = mar,
                headPoseYaw = yaw,
                headPosePitch = pitch,
                frameCount = consecutiveClosedEyes,
                message = "Drowsiness detected! Please take a break"
            )
            consecutiveClosedEyes > EAR_CONSECUTIVE_FRAMES / 2 -> DrowsinessDetails(
                level = DrowsinessLevel.DROWSY,
                earValue = ear,
                marValue = mar,
                headPoseYaw = yaw,
                headPosePitch = pitch,
                frameCount = consecutiveClosedEyes,
                message = "You seem tired. Consider taking a break"
            )
            consecutiveYawns > YAWN_FRAMES -> DrowsinessDetails(
                level = DrowsinessLevel.YAWNING,
                earValue = ear,
                marValue = mar,
                headPoseYaw = yaw,
                headPosePitch = pitch,
                frameCount = consecutiveYawns,
                message = "Yawning detected. Stay alert!"
            )
            else -> DrowsinessDetails(
                level = DrowsinessLevel.ALERT,
                earValue = ear,
                marValue = mar,
                headPoseYaw = yaw,
                headPosePitch = pitch,
                frameCount = 0,
                message = "Driver alert"
            )
        }
    }

    fun close() {
        // ML Kit detector doesn't need explicit closing
        consecutiveClosedEyes = 0
        consecutiveYawns = 0
    }
}
