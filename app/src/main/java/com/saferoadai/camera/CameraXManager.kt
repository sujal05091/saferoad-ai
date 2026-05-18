package com.saferoadai.camera

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX Manager for handling single or dual camera operation
 * Supports front camera (drowsiness detection) and rear camera (road hazard detection)
 */
class CameraXManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraXManager"
        private val CAMERA_RESOLUTION = Size(1280, 720)  // 720p for performance
    }

    enum class CameraMode {
        FRONT_ONLY,
        REAR_ONLY,
        DUAL  // Attempt both, fallback to rear if not supported
    }

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var frontCamera: Camera? = null
    private var rearCamera: Camera? = null
    
    private var frontPreview: Preview? = null
    private var rearPreview: Preview? = null
    
    private var frontImageAnalysis: ImageAnalysis? = null
    private var rearImageAnalysis: ImageAnalysis? = null
    
    // Store last frame for capture
    private var lastRearFrame: android.graphics.Bitmap? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initialize camera provider
     */
    fun initialize(onReady: () -> Unit, onError: (Exception) -> Unit) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture?.addListener({
            try {
                cameraProvider = cameraProviderFuture?.get()
                Log.i(TAG, "CameraProvider initialized")
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraProvider", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Start camera(s) based on selected mode
     */
    fun startCamera(
        mode: CameraMode,
        lifecycleOwner: LifecycleOwner,
        frontPreviewView: PreviewView? = null,
        rearPreviewView: PreviewView? = null,
        frontAnalyzer: ImageAnalysis.Analyzer? = null,
        rearAnalyzer: ImageAnalysis.Analyzer? = null
    ): Boolean {
        val provider = cameraProvider ?: run {
            Log.e(TAG, "CameraProvider not initialized")
            return false
        }

        // Unbind all use cases before rebinding
        provider.unbindAll()

        return when (mode) {
            CameraMode.FRONT_ONLY -> startFrontCamera(
                provider, lifecycleOwner, frontPreviewView, frontAnalyzer
            )
            CameraMode.REAR_ONLY -> startRearCamera(
                provider, lifecycleOwner, rearPreviewView, rearAnalyzer
            )
            CameraMode.DUAL -> startDualCamera(
                provider, lifecycleOwner, frontPreviewView, rearPreviewView, 
                frontAnalyzer, rearAnalyzer
            )
        }
    }

    /**
     * Start front camera only (for drowsiness detection)
     */
    private fun startFrontCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        analyzer: ImageAnalysis.Analyzer?
    ): Boolean {
        return try {
            // Preview
            frontPreview = Preview.Builder()
                .setTargetResolution(CAMERA_RESOLUTION)
                .build()
            
            previewView?.let { frontPreview?.setSurfaceProvider(it.surfaceProvider) }

            // Image Analysis
            frontImageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(CAMERA_RESOLUTION)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            analyzer?.let { frontImageAnalysis?.setAnalyzer(cameraExecutor, it) }

            // Bind to lifecycle
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            frontCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                frontPreview,
                frontImageAnalysis
            )

            Log.i(TAG, "Front camera started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start front camera", e)
            false
        }
    }

    /**
     * Start rear camera only (for road hazard/sign detection)
     */
    private fun startRearCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?,
        analyzer: ImageAnalysis.Analyzer?
    ): Boolean {
        return try {
            // Preview
            rearPreview = Preview.Builder()
                .setTargetResolution(CAMERA_RESOLUTION)
                .build()
            
            previewView?.let { rearPreview?.setSurfaceProvider(it.surfaceProvider) }

            // Image Analysis
            rearImageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(CAMERA_RESOLUTION)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            analyzer?.let { rearImageAnalysis?.setAnalyzer(cameraExecutor, it) }

            // Bind to lifecycle
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            rearCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                rearPreview,
                rearImageAnalysis
            )

            Log.i(TAG, "Rear camera started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start rear camera", e)
            false
        }
    }

    /**
     * Start both cameras simultaneously (if device supports it)
     * Falls back to rear camera only if dual camera not supported
     */
    private fun startDualCamera(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        frontPreviewView: PreviewView?,
        rearPreviewView: PreviewView?,
        frontAnalyzer: ImageAnalysis.Analyzer?,
        rearAnalyzer: ImageAnalysis.Analyzer?
    ): Boolean {
        // Check if device supports concurrent cameras
        val concurrentCamera = provider.availableConcurrentCameraInfos
        val canUseDual = concurrentCamera.isNotEmpty()

        if (!canUseDual) {
            Log.w(TAG, "Device doesn't support concurrent cameras, falling back to rear only")
            return startRearCamera(provider, lifecycleOwner, rearPreviewView, rearAnalyzer)
        }

        return try {
            // Setup front camera
            val frontSuccess = startFrontCamera(
                provider, lifecycleOwner, frontPreviewView, frontAnalyzer
            )
            
            // Setup rear camera
            val rearSuccess = startRearCamera(
                provider, lifecycleOwner, rearPreviewView, rearAnalyzer
            )

            if (frontSuccess && rearSuccess) {
                Log.i(TAG, "Dual cameras started")
                true
            } else {
                Log.w(TAG, "Failed to start both cameras, falling back")
                provider.unbindAll()
                startRearCamera(provider, lifecycleOwner, rearPreviewView, rearAnalyzer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start dual cameras", e)
            // Fallback to rear camera
            provider.unbindAll()
            startRearCamera(provider, lifecycleOwner, rearPreviewView, rearAnalyzer)
        }
    }

    /**
     * Stop all cameras
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        frontCamera = null
        rearCamera = null
        Log.i(TAG, "Cameras stopped")
    }

    /**
     * Enable/disable torch on rear camera
     */
    fun setTorch(enabled: Boolean) {
        rearCamera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * Get camera info
     */
    fun isFrontCameraAvailable(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
    }

    fun isRearCameraAvailable(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
    }

    fun isDualCameraSupported(): Boolean {
        val concurrent = cameraProvider?.availableConcurrentCameraInfos ?: emptyList()
        return concurrent.isNotEmpty()
    }
    
    /**
     * Capture current frame from rear camera
     * @param callback Called with captured bitmap (or null if failed)
     */
    fun captureImage(callback: (android.graphics.Bitmap?) -> Unit) {
        callback(lastRearFrame)
    }
    
    /**
     * Update last captured frame (call this in analyzer)
     */
    fun updateLastFrame(bitmap: android.graphics.Bitmap) {
        lastRearFrame?.recycle()  // Free old bitmap
        lastRearFrame = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Release resources
     */
    fun release() {
        stopCamera()
        cameraExecutor.shutdown()
        Log.i(TAG, "CameraXManager released")
    }
}
