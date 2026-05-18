package com.saferoadai.detectors

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Generic TensorFlow Lite object detection base class
 * Handles model loading, preprocessing, inference, and NMS (Non-Maximum Suppression)
 */
open class TFLiteDetector(
    private val context: Context,
    private val modelPath: String,
    private val inputSize: Int = 640,
    private val numThreads: Int = 4,
    private val useGpu: Boolean = false,
    private val numClasses: Int = 80  // Default 80 classes for COCO
) {
    companion object {
        private const val TAG = "TFLiteDetector"
        // 1% threshold - required for weak model (actual confidence: 1.3-1.38%)
        // False positive prevention: increased cooldown + bbox size filtering
        private const val CONFIDENCE_THRESHOLD = 0.01f  // 1% - weak model compatibility
        private const val IOU_THRESHOLD = 0.45f
    }

    protected var interpreter: Interpreter? = null
    protected var isInitialized = false

    data class Detection(
        val bbox: RectF,  // Bounding box (left, top, right, bottom)
        val confidence: Float,
        val classId: Int,
        val className: String
    )

    /**
     * Initialize TFLite interpreter
     * TODO: If model missing, this will fail - handle gracefully in UI
     */
    fun initialize(): Boolean {
        return try {
            val options = Interpreter.Options().apply {
                numThreads = this@TFLiteDetector.numThreads
                // TODO: Enable GPU delegate if useGpu = true
                // if (useGpu) addDelegate(GpuDelegate())
            }
            
            val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(modelBuffer, options)
            
            isInitialized = true
            Log.i(TAG, "TFLite model loaded successfully: $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite model: $modelPath", e)
            isInitialized = false
            false
        }
    }

    /**
     * Run object detection on input bitmap
     * @return List of Detection objects
     */
    open fun detect(bitmap: Bitmap): List<Detection> {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "❌ Detector not initialized. Call initialize() first.")
            return emptyList()
        }

        return try {
            Log.d(TAG, "🔍 Starting detection on ${bitmap.width}x${bitmap.height} image")
            
            // Preprocess image
            val tensorImage = preprocessImage(bitmap)
            Log.d(TAG, "✅ Image preprocessed to ${inputSize}x${inputSize}")
            
            // Prepare output buffers (YOLOv8 format: [1, numChannels, numDetections])
            // numChannels = 4 (bbox) + numClasses
            val numChannels = 4 + numClasses
            val outputArray = Array(1) { Array(numChannels) { FloatArray(8400) } }
            Log.d(TAG, "📊 Output buffer shape: [1, $numChannels, 8400]")
            
            // Run inference
            Log.d(TAG, "🧠 Running model inference...")
            interpreter?.run(tensorImage.buffer, outputArray)
            Log.d(TAG, "✅ Inference complete")
            
            // Parse detections
            val rawDetections = parseOutput(outputArray, bitmap.width, bitmap.height)
            Log.d(TAG, "📊 Found ${rawDetections.size} raw detections")
            
            // Apply Non-Maximum Suppression
            val finalDetections = applyNMS(rawDetections)
            Log.d(TAG, "✅ After NMS: ${finalDetections.size} detections")
            
            finalDetections
        } catch (e: Exception) {
            Log.e(TAG, "💥 Detection failed", e)
            emptyList()
        }
    }

    /**
     * Preprocess input image for model
     */
    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))  // Normalize to [0,1] - required for TFLite models
            .build()

        // Use FLOAT32 DataType for proper model input
        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)
        return tensorImage
    }

    /**
     * Parse model output into Detection objects
     * Override this method for specific model output format
     */
    protected open fun parseOutput(
        output: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        
        Log.d(TAG, "🔬 Output shape: [${output.size}, ${output[0].size}, ${output[0][0].size}]")
        
        // YOLOv8 format parsing
        // output shape: [1, (4+numClasses), 8400] where first 4 are bbox coords
        val numDetections = output[0][0].size
        val actualNumClasses = output[0].size - 4
        Log.d(TAG, "🔍 Model has $actualNumClasses classes (expected $numClasses)")
        
        Log.d(TAG, "📊 Parsing $numDetections detections, $numClasses classes, threshold=$CONFIDENCE_THRESHOLD")
        
        var aboveThresholdCount = 0
        var maxConfidenceFound = 0f
        var maxConfidenceClass = -1
        
        for (i in 0 until numDetections) {
            // Extract bbox coordinates (center_x, center_y, width, height)
            val cx = output[0][0][i]
            val cy = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]
            
            // Find best class and confidence
            var maxConf = 0f
            var classId = 0
            for (c in 4 until output[0].size) {
                if (output[0][c][i] > maxConf) {
                    maxConf = output[0][c][i]
                    classId = c - 4
                }
            }
            
            // Track highest confidence across all detections
            if (maxConf > maxConfidenceFound) {
                maxConfidenceFound = maxConf
                maxConfidenceClass = classId
            }
            
            if (maxConf >= CONFIDENCE_THRESHOLD) {
                aboveThresholdCount++
                // Convert to corner format
                val left = (cx - w / 2) * originalWidth / inputSize
                val top = (cy - h / 2) * originalHeight / inputSize
                val right = (cx + w / 2) * originalWidth / inputSize
                val bottom = (cy + h / 2) * originalHeight / inputSize
                
                detections.add(
                    Detection(
                        bbox = RectF(left, top, right, bottom),
                        confidence = maxConf,
                        classId = classId,
                        className = getClassName(classId)
                    )
                )
            }
        }
        
        Log.d(TAG, "🔍 Max confidence found: $maxConfidenceFound for class $maxConfidenceClass (${getClassName(maxConfidenceClass)})")
        Log.d(TAG, "✅ Found $aboveThresholdCount detections above threshold, keeping ${detections.size} after filtering")
        
        return detections
    }

    /**
     * Apply Non-Maximum Suppression to remove overlapping boxes
     */
    private fun applyNMS(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()
        
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<Detection>()
        
        for (detection in sortedDetections) {
            var shouldSelect = true
            for (selected in selectedDetections) {
                if (calculateIoU(detection.bbox, selected.bbox) > IOU_THRESHOLD) {
                    shouldSelect = false
                    break
                }
            }
            if (shouldSelect) {
                selectedDetections.add(detection)
            }
        }
        
        return selectedDetections
    }

    /**
     * Calculate Intersection over Union
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionRight < intersectionLeft || intersectionBottom < intersectionTop) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return intersectionArea / unionArea
    }

    /**
     * Get class name from class ID
     * Override this in subclasses with specific labels
     */
    protected open fun getClassName(classId: Int): String {
        return "object_$classId"
    }

    /**
     * Release resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Log.i(TAG, "Detector closed")
    }
}
