package com.saferoadai.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.saferoadai.R
import com.saferoadai.audio.AlertManager
import com.saferoadai.camera.CameraXManager
import com.saferoadai.db.AppDatabase
import com.saferoadai.db.DrowsinessEvent
import com.saferoadai.db.HazardEvent
import com.saferoadai.db.SignEvent
import com.saferoadai.detectors.RoadHazardDetector
import com.saferoadai.detectors.TrafficSignDetector
import com.saferoadai.face.FaceMonitorAnalyzer
import com.saferoadai.firebase.FirebaseManager
import com.saferoadai.location.LocationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * Main ride activity - handles detection during driving
 * Manages dual cameras, detection pipelines, alerts, and data storage
 */
class RideActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RideActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // UI Components
    private lateinit var frontPreviewView: PreviewView
    private lateinit var rearPreviewView: PreviewView
    private lateinit var startStopButton: MaterialButton
    private lateinit var detectingAnimation: LottieAnimationView
    private lateinit var overlayDetectingAnimation: LottieAnimationView
    private lateinit var cameraContainer: android.view.ViewGroup
    private lateinit var initialScreen: android.view.ViewGroup
    private lateinit var detectionCard: MaterialCardView
    private lateinit var detectionTypeText: TextView
    private lateinit var detectionDetailsText: TextView
    
    // Managers
    private lateinit var cameraManager: CameraXManager
    private lateinit var locationManager: LocationManager
    private lateinit var alertManager: AlertManager
    private lateinit var firebaseManager: FirebaseManager
    
    // Detectors
    private lateinit var hazardDetector: RoadHazardDetector
    private lateinit var roboflowDetector: com.saferoadai.detectors.RoboflowDetector  // Cloud API (more accurate)
    private lateinit var signDetector: TrafficSignDetector
    private lateinit var faceAnalyzer: FaceMonitorAnalyzer
    
    // Detection mode
    private var useRoboflow = true  // Use Roboflow by default (better accuracy)
    
    // Database
    private lateinit var database: AppDatabase
    
    // State
    private var isRiding = false
    // DUAL mode: Back camera for road detection + Front camera for driver monitoring
    private var selectedCameraMode = CameraXManager.CameraMode.DUAL
    private val rideId = UUID.randomUUID().toString()
    
    // No cooldown needed - Roboflow API is accurate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride)
        
        initializeViews()
        initializeManagers()
        initializeDetectors()
        
        if (checkPermissions()) {
            setupCameraMode()
        } else {
            requestPermissions()
        }
    }

    private fun initializeViews() {
        frontPreviewView = findViewById(R.id.frontPreviewView)
        rearPreviewView = findViewById(R.id.rearPreviewView)
        startStopButton = findViewById(R.id.startStopButton)
        detectingAnimation = findViewById(R.id.detectingAnimation)
        overlayDetectingAnimation = findViewById(R.id.overlayDetectingAnimation)
        cameraContainer = findViewById(R.id.cameraContainer)
        initialScreen = findViewById(R.id.initialScreen)
        detectionCard = findViewById(R.id.detectionCard)
        detectionTypeText = findViewById(R.id.detectionTypeText)
        detectionDetailsText = findViewById(R.id.detectionDetailsText)
        
        startStopButton.setOnClickListener {
            if (isRiding) stopRide() else showCameraModeDialog()
        }
    }

    private fun initializeManagers() {
        // Initialize Cloudinary for image uploads
        com.saferoadai.utils.CloudinaryManager.init(this)
        
        cameraManager = CameraXManager(this)
        locationManager = LocationManager(this)
        alertManager = AlertManager(this)
        firebaseManager = FirebaseManager()
        database = AppDatabase.getDatabase(this)
        
        // Initialize camera provider
        cameraManager.initialize(
            onReady = {
                Log.i(TAG, "Camera provider ready")
            },
            onError = { error ->
                Log.e(TAG, "Camera initialization failed", error)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun initializeDetectors() {
        // Initialize Roboflow detector (cloud API - more accurate)
        roboflowDetector = com.saferoadai.detectors.RoboflowDetector()
        Log.i(TAG, "✅ Roboflow detector initialized")
        
        // Initialize TFLite detectors (fallback for offline use)
        try {
            val hazard = RoadHazardDetector(this)
            val hazardInitialized = hazard.initialize()
            
            if (hazardInitialized) {
                hazardDetector = hazard
                Log.i(TAG, "✅ Local hazard detector initialized (fallback)")
                Toast.makeText(this, "✅ Detection ready (Roboflow + Local)", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "❌ Hazard detector initialization failed")
                Toast.makeText(this, "✅ Roboflow only (local model failed)", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating hazard detector", e)
            Toast.makeText(this, "✅ Roboflow only (local error)", Toast.LENGTH_LONG).show()
        }
        
        try {
            val sign = TrafficSignDetector(this)
            val signInitialized = sign.initialize()
            
            if (signInitialized) {
                signDetector = sign
                Log.i(TAG, "✅ Sign detector initialized successfully")
                Toast.makeText(this, "✅ Sign model loaded", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "❌ Sign detector initialization failed")
                Toast.makeText(this, "❌ Sign model failed to load", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating sign detector", e)
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Face analyzer
        faceAnalyzer = FaceMonitorAnalyzer(this) { level, details ->
            handleDrowsinessDetection(level, details)
        }
    }

    private fun setupCameraMode() {
        // This is now called after mode selection
        configureCameraLayout()
        startRide()
    }
    
    private fun showCameraModeDialog() {
        // Show dialog to select camera mode
        val options = arrayOf("Front Camera Only", "Rear Camera Only", "Dual Cameras")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Camera Mode")
            .setItems(options) { _, which ->
                selectedCameraMode = when (which) {
                    0 -> CameraXManager.CameraMode.FRONT_ONLY
                    1 -> CameraXManager.CameraMode.REAR_ONLY
                    else -> CameraXManager.CameraMode.DUAL
                }
                Log.i(TAG, "Camera mode selected: $selectedCameraMode")
                setupCameraMode()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun configureCameraLayout() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        
        when (selectedCameraMode) {
            CameraXManager.CameraMode.FRONT_ONLY -> {
                // Front camera full screen
                frontPreviewView.visibility = android.view.View.VISIBLE
                frontPreviewView.layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                rearPreviewView.visibility = android.view.View.GONE
            }
            CameraXManager.CameraMode.REAR_ONLY -> {
                // Rear camera full screen
                rearPreviewView.visibility = android.view.View.VISIBLE
                frontPreviewView.visibility = android.view.View.GONE
            }
            CameraXManager.CameraMode.DUAL -> {
                // Split screen: 1/4 front (top), 3/4 rear (bottom)
                val frontHeight = screenHeight / 4
                val rearHeight = (screenHeight * 3) / 4
                
                frontPreviewView.visibility = android.view.View.VISIBLE
                frontPreviewView.layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    frontHeight
                ).apply {
                    gravity = android.view.Gravity.TOP
                }
                
                rearPreviewView.visibility = android.view.View.VISIBLE
                rearPreviewView.layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    rearHeight
                ).apply {
                    gravity = android.view.Gravity.BOTTOM
                }
            }
        }
    }

    private fun startRide() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        
        isRiding = true
        
        // Hide initial screen, show camera container
        initialScreen.visibility = android.view.View.GONE
        cameraContainer.visibility = android.view.View.VISIBLE
        
        // Show overlay detecting animation
        overlayDetectingAnimation.visibility = android.view.View.VISIBLE
        overlayDetectingAnimation.playAnimation()
        
        // Update button
        startStopButton.text = "STOP RIDE"
        startStopButton.icon = getDrawable(android.R.drawable.ic_media_pause)
        
        // Start location updates
        lifecycleScope.launch {
            locationManager.startLocationUpdates().collectLatest { location ->
                Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
            }
        }
        
        // Start camera
        val success = cameraManager.startCamera(
            mode = selectedCameraMode,
            lifecycleOwner = this,
            frontPreviewView = if (selectedCameraMode != CameraXManager.CameraMode.REAR_ONLY) frontPreviewView else null,
            rearPreviewView = if (selectedCameraMode != CameraXManager.CameraMode.FRONT_ONLY) rearPreviewView else null,
            frontAnalyzer = faceAnalyzer,
            rearAnalyzer = createRoadAnalyzer()
        )
        
        if (!success) {
            Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            stopRide()
        } else {
            Toast.makeText(this, "Ride started - Stay safe!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRide() {
        isRiding = false
        
        // Show initial screen, hide camera container
        initialScreen.visibility = android.view.View.VISIBLE
        cameraContainer.visibility = android.view.View.GONE
        
        // Hide overlay detecting animation
        overlayDetectingAnimation.visibility = android.view.View.GONE
        overlayDetectingAnimation.pauseAnimation()
        
        // Update button
        startStopButton.text = "START RIDE"
        startStopButton.icon = getDrawable(android.R.drawable.ic_media_play)
        
        cameraManager.stopCamera()
        locationManager.stopLocationUpdates()
        alertManager.stop()
        
        Toast.makeText(this, "Ride stopped", Toast.LENGTH_SHORT).show()
    }

    /**
     * Create image analyzer for rear camera (hazards + signs)
     */
    private fun createRoadAnalyzer(): androidx.camera.core.ImageAnalysis.Analyzer {
        return androidx.camera.core.ImageAnalysis.Analyzer { imageProxy ->
            try {
                Log.d(TAG, "📸 Processing frame ${imageProxy.width}x${imageProxy.height}")
                
                // Convert ImageProxy to Bitmap
                val bitmap = imageProxy.toBitmap()
                Log.d(TAG, "🖼️ Bitmap created: ${bitmap.width}x${bitmap.height}")
                
                // Save frame for capture
                cameraManager.updateLastFrame(bitmap)
                
                val currentLocation = locationManager.getCurrentLocation()
                Log.d(TAG, "📍 Location: ${if (currentLocation != null) "Available" else "Unavailable"}")
                
                // Detect hazards using Roboflow (with local fallback)
                lifecycleScope.launch {
                    try {
                        // Try Roboflow first (more accurate)
                        if (useRoboflow) {
                            Log.d(TAG, "🌐 Running Roboflow detection...")
                            val roboflowDetections = roboflowDetector.detectPotholes(bitmap)
                            
                            if (roboflowDetections.isNotEmpty()) {
                                val topDetection = roboflowDetections.first()
                                val confidencePercent = (topDetection.confidence * 100).toInt()
                                
                                Log.i(TAG, "⚠️ ROBOFLOW POTHOLE: ${topDetection.className} ($confidencePercent%)")
                                
                                // Show on screen
                                runOnUiThread {
                                    showDetectionOnScreen(
                                        "⚠️ Pothole (Roboflow)",
                                        "Confidence: $confidencePercent%",
                                        isHazard = true
                                    )
                                }
                                
                                // Save to database if location available
                                if (currentLocation != null) {
                                    handleRoboflowDetection(topDetection, currentLocation)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Roboflow failed, using local model: ${e.message}")
                        // Fallback to local model
                        if (hazardDetector != null) {
                            val hazards = hazardDetector!!.detectHazards(bitmap)
                            if (hazards.isNotEmpty()) {
                                val topHazard = hazards.first()
                                val confidencePercent = (topHazard.confidence * 100).toInt()
                                
                                Log.i(TAG, "⚠️ LOCAL HAZARD: ${topHazard.type} ($confidencePercent%)")
                                
                                runOnUiThread {
                                    showDetectionOnScreen(
                                        "⚠️ ${topHazard.type} (Local)",
                                        "Confidence: $confidencePercent%",
                                        isHazard = true
                                    )
                                }
                                
                                if (currentLocation != null) {
                                    handleHazardDetection(topHazard, currentLocation)
                                }
                            }
                        }
                    }
                }
                
                // Detect traffic signs
                if (signDetector != null) {
                    Log.d(TAG, "🔍 Running sign detection...")
                    val signs = signDetector!!.detectSigns(bitmap)
                    Log.d(TAG, "📊 Detected ${signs.size} traffic signs")
                    
                    if (signs.isNotEmpty()) {
                        val topSign = signs.first()  // Already sorted by importance
                        val confidencePercent = (topSign.confidence * 100).toInt()
                        
                        // Check if user is moving for speed limit signs
                        val isMoving = currentLocation?.speed ?: 0f > 1.0f  // Moving if speed > 1 m/s (~3.6 km/h)
                        val shouldShowSign = if (topSign.type.contains("Speed Limit")) {
                            isMoving  // Only show speed limit if moving
                        } else {
                                true  // Show all other signs regardless of speed
                            }
                        
                        if (shouldShowSign) {
                            Log.i(TAG, "🚦 SIGN: ${topSign.type} (${topSign.confidence}) - Confidence: $confidencePercent%")
                            
                            // Show on screen
                            runOnUiThread {
                                showDetectionOnScreen(
                                    "🚦 ${topSign.type}",
                                    "Confidence: $confidencePercent% | Importance: ${topSign.importance}/10",
                                    isHazard = false
                                )
                            }
                            
                            // Save to database if location available
                            if (currentLocation != null) {
                                handleSignDetection(topSign, currentLocation)
                            }
                        } else {
                            Log.d(TAG, "⏸️ Skipping Speed Limit sign (not moving: ${currentLocation?.speed ?: 0f} m/s)")
                        }
                    }
                } else {
                    Log.w(TAG, "❌ Sign detector is NULL")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error in detection analyzer", e)
            } finally {
                imageProxy.close()
            }
        }
    }
    
    /**
     * Convert ImageProxy to Bitmap
     */
    private fun androidx.camera.core.ImageProxy.toBitmap(): android.graphics.Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * Show detection on screen with card display
     */
    private fun showDetectionOnScreen(title: String, details: String, isHazard: Boolean) {
        detectionTypeText.text = title
        detectionDetailsText.text = details
        
        // Change color based on type
        val color = if (isHazard) {
            resources.getColor(android.R.color.holo_red_dark, null)
        } else {
            resources.getColor(android.R.color.holo_blue_dark, null)
        }
        detectionTypeText.setTextColor(color)
        
        // Show card
        detectionCard.visibility = android.view.View.VISIBLE
        
        // Auto-hide after 5 seconds
        detectionCard.postDelayed({
            detectionCard.visibility = android.view.View.GONE
        }, 5000)
    }
    
    /**
     * Handle Roboflow pothole detection with image upload
     */
    private fun handleRoboflowDetection(
        detection: com.saferoadai.detectors.RoboflowDetector.Detection,
        location: LocationManager.LocationData
    ) {
        Log.i(TAG, "Roboflow pothole detected: ${detection.className} (${detection.confidence})")
        
        // Alert user (Roboflow is accurate, so always alert)
        alertManager.alertHazard("Pothole", "HIGH")
        
        // Save to database
        val event = HazardEvent(
            type = "Pothole (Roboflow)",
            latitude = location.latitude,
            longitude = location.longitude,
            timestampFirstSeen = System.currentTimeMillis(),
            timestampLastSeen = System.currentTimeMillis(),
            confidence = detection.confidence,
            severity = "HIGH",
            status = "ACTIVE"
        )
        
        lifecycleScope.launch {
            val id = database.eventDao().insertHazard(event)
            
            // Capture and upload image to Cloudinary
            cameraManager.captureImage { bitmap ->
                if (bitmap != null) {
                    lifecycleScope.launch {
                        try {
                            val tempFile = java.io.File(cacheDir, "roboflow_pothole_${id}_${System.currentTimeMillis()}.jpg")
                            tempFile.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            
                            val imageUrl = com.saferoadai.utils.CloudinaryManager.uploadFile(
                                file = tempFile,
                                folder = "saferoadai/hazards_roboflow/${rideId}",
                                publicId = "pothole_${id}"
                            )
                            
                            Log.i(TAG, "Roboflow pothole image uploaded: $imageUrl")
                            tempFile.delete()
                            
                            firebaseManager.uploadHazard(event.copy(id = id))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to upload Roboflow detection: ${e.message}")
                            firebaseManager.uploadHazard(event.copy(id = id))
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle drowsiness detection callback
     */
    private fun handleDrowsinessDetection(
        level: FaceMonitorAnalyzer.DrowsinessLevel,
        details: FaceMonitorAnalyzer.DrowsinessDetails
    ) {
        if (level == FaceMonitorAnalyzer.DrowsinessLevel.ALERT) return
        
        Log.w(TAG, "Drowsiness detected: $level - ${details.message}")
        
        // Alert user
        alertManager.alertDrowsiness(level.name)
        
        // Save to database
        val currentLocation = locationManager.getCurrentLocation()
        if (currentLocation != null) {
            val event = DrowsinessEvent(
                level = level.name,
                earValue = details.earValue,
                marValue = details.marValue,
                headPoseYaw = details.headPoseYaw,
                headPosePitch = details.headPosePitch,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                timestamp = System.currentTimeMillis(),
                message = details.message,
                rideId = rideId
            )
            
            lifecycleScope.launch {
                database.eventDao().insertDrowsiness(event)
            }
        }
    }

    /**
     * Handle hazard detection with image capture and upload
     */
    private fun handleHazardDetection(
        hazard: RoadHazardDetector.HazardDetectionResult,
        location: LocationManager.LocationData
    ) {
        Log.i(TAG, "Hazard detected: ${hazard.type} (${hazard.confidence})")
        
        // Alert if severe
        if (hazardDetector.shouldAlert(hazard)) {
            alertManager.alertHazard(hazard.type, hazard.severity.name)
        }
        
        // Save to database
        val event = HazardEvent(
            type = hazard.type,
            latitude = location.latitude,
            longitude = location.longitude,
            timestampFirstSeen = System.currentTimeMillis(),
            timestampLastSeen = System.currentTimeMillis(),
            confidence = hazard.confidence,
            severity = hazard.severity.name,
            status = "ACTIVE"
        )
        
        lifecycleScope.launch {
            val id = database.eventDao().insertHazard(event)
            
            // Capture and upload image to Cloudinary
            cameraManager.captureImage { bitmap ->
                if (bitmap != null) {
                    lifecycleScope.launch {
                        try {
                            // Save bitmap to temp file
                            val tempFile = java.io.File(cacheDir, "hazard_${id}_${System.currentTimeMillis()}.jpg")
                            tempFile.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            
                            // Upload to Cloudinary
                            val imageUrl = com.saferoadai.utils.CloudinaryManager.uploadFile(
                                file = tempFile,
                                folder = "saferoadai/hazards/${rideId}",
                                publicId = "hazard_${id}"
                            )
                            
                            Log.i(TAG, "Hazard image uploaded to Cloudinary: $imageUrl")
                            tempFile.delete()  // Clean up temp file
                            
                            // Upload hazard data to Firebase
                            firebaseManager.uploadHazard(event.copy(id = id))
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to upload hazard image: ${e.message}")
                            // Still upload hazard data without image
                            firebaseManager.uploadHazard(event.copy(id = id))
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle sign detection with image capture and upload
     */
    private fun handleSignDetection(
        sign: TrafficSignDetector.SignDetectionResult,
        location: LocationManager.LocationData
    ) {
        Log.i(TAG, "Sign detected: ${sign.type} (${sign.confidence})")
        
        // Alert for important signs
        if (sign.importance >= 7) {
            alertManager.alertTrafficSign(sign.type)
        }
        
        // Save to database
        val event = SignEvent(
            type = sign.type,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            confidence = sign.confidence,
            importance = sign.importance
        )
        
        lifecycleScope.launch {
            val id = database.eventDao().insertSign(event)
            
            // Capture and upload image to Cloudinary
            cameraManager.captureImage { bitmap ->
                if (bitmap != null) {
                    lifecycleScope.launch {
                        try {
                            // Save bitmap to temp file
                            val tempFile = java.io.File(cacheDir, "sign_${id}_${System.currentTimeMillis()}.jpg")
                            tempFile.outputStream().use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            
                            // Upload to Cloudinary
                            val imageUrl = com.saferoadai.utils.CloudinaryManager.uploadFile(
                                file = tempFile,
                                folder = "saferoadai/signs/${rideId}",
                                publicId = "sign_${id}"
                            )
                            
                            Log.i(TAG, "Sign image uploaded to Cloudinary: $imageUrl")
                            tempFile.delete()  // Clean up temp file
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to upload sign image: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupCameraMode()
            } else {
                Toast.makeText(this, "Permissions required to use app", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
        locationManager.stopLocationUpdates()
        alertManager.shutdown()
        hazardDetector.close()
        signDetector.close()
        faceAnalyzer.close()
    }
}
