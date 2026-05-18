package com.saferoadai.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.saferoadai.R
import com.saferoadai.detectors.RoadHazardDetector
import com.saferoadai.detectors.TrafficSignDetector

/**
 * Activity to test if both TFLite models load correctly
 */
class ModelTestActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_test)

        resultText = findViewById(R.id.resultText)
        testButton = findViewById(R.id.testButton)

        testButton.setOnClickListener {
            testModels()
        }
    }

    private fun testModels() {
        val results = StringBuilder()
        results.append("🧪 Testing Models...\n\n")

        // Test Hazard Detector (Pothole Model)
        results.append("1️⃣ Pothole Detection Model:\n")
        try {
            val hazardDetector = RoadHazardDetector(this)
            results.append("✅ SUCCESS - patholes.tflite loaded!\n")
            results.append("   Model ready for detection\n\n")
        } catch (e: Exception) {
            results.append("❌ FAILED - Error loading model\n")
            results.append("   Error: ${e.message}\n\n")
        }

        // Test Traffic Sign Detector
        results.append("2️⃣ Traffic Sign Detection Model:\n")
        try {
            val signDetector = TrafficSignDetector(this)
            results.append("✅ SUCCESS - traffic_signs.tflite loaded!\n")
            results.append("   Model ready for detection\n\n")
        } catch (e: Exception) {
            results.append("❌ FAILED - Error loading model\n")
            results.append("   Error: ${e.message}\n\n")
        }

        results.append("\n📊 Test Complete!")
        resultText.text = results.toString()
    }
}
