package com.saferoadai.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

/**
 * Alert Manager for audio notifications using Text-to-Speech
 * Implements rate limiting to prevent alert spam
 */
class AlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AlertManager"
        private const val MIN_ALERT_INTERVAL_MS = 5000L  // 5 seconds between same alert
        private const val CRITICAL_ALERT_INTERVAL_MS = 2000L  // 2 seconds for critical
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isEnabled = true
    
    private val lastAlertTimes = mutableMapOf<String, Long>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language not supported")
                isTtsInitialized = false
            } else {
                isTtsInitialized = true
                tts?.setSpeechRate(1.1f)  // Slightly faster for urgency
                Log.i(TAG, "TTS initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
            isTtsInitialized = false
        }
    }

    /**
     * Speak alert message
     * @param message Text to speak
     * @param alertType Type of alert for rate limiting
     * @param priority Priority (use QUEUE_FLUSH for critical, QUEUE_ADD for normal)
     */
    fun speak(
        message: String,
        alertType: AlertType = AlertType.GENERAL,
        priority: Priority = Priority.NORMAL
    ) {
        if (!isEnabled || !isTtsInitialized) {
            Log.d(TAG, "TTS disabled or not initialized")
            return
        }

        // Rate limiting
        val currentTime = System.currentTimeMillis()
        val lastAlertTime = lastAlertTimes[alertType.name] ?: 0L
        val minInterval = if (priority == Priority.CRITICAL) {
            CRITICAL_ALERT_INTERVAL_MS
        } else {
            MIN_ALERT_INTERVAL_MS
        }

        if (currentTime - lastAlertTime < minInterval) {
            Log.d(TAG, "Alert rate limited: $alertType")
            return
        }

        // Speak message
        val queueMode = if (priority == Priority.CRITICAL) {
            TextToSpeech.QUEUE_FLUSH  // Interrupt current speech
        } else {
            TextToSpeech.QUEUE_ADD  // Queue after current
        }

        tts?.speak(message, queueMode, null, alertType.name)
        lastAlertTimes[alertType.name] = currentTime
        Log.i(TAG, "Alert spoken: $message")
    }

    /**
     * Quick alert methods for common scenarios
     */
    fun alertDrowsiness(level: String) {
        val message = when (level) {
            "VERY_DROWSY" -> "Warning! Drowsiness detected. Please take a break immediately."
            "DROWSY" -> "You seem tired. Consider taking a break soon."
            "YAWNING" -> "Yawning detected. Stay alert."
            "DISTRACTED" -> "Please keep your eyes on the road."
            else -> "Stay alert while driving."
        }
        speak(message, AlertType.DROWSINESS, Priority.CRITICAL)
    }

    fun alertHazard(hazardType: String, severity: String) {
        val message = when (severity) {
            "CRITICAL" -> "Critical hazard ahead! $hazardType detected."
            "HIGH" -> "Caution! $hazardType ahead."
            "MEDIUM" -> "$hazardType detected on road."
            else -> "Road condition alert."
        }
        val priority = if (severity == "CRITICAL" || severity == "HIGH") {
            Priority.CRITICAL
        } else {
            Priority.NORMAL
        }
        speak(message, AlertType.HAZARD, priority)
    }

    fun alertTrafficSign(signType: String) {
        val message = when {
            signType.contains("Stop") -> "Stop sign ahead."
            signType.contains("Speed Limit") -> "Speed limit: $signType"
            signType.contains("Yield") -> "Yield ahead."
            signType.contains("Caution") -> "Caution: $signType"
            else -> "Traffic sign detected: $signType"
        }
        speak(message, AlertType.TRAFFIC_SIGN, Priority.NORMAL)
    }

    /**
     * Enable/disable alerts
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            stop()
        }
        Log.i(TAG, "Alerts ${if (enabled) "enabled" else "disabled"}")
    }

    fun isEnabled(): Boolean = isEnabled

    /**
     * Stop current speech
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Release TTS resources
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        Log.i(TAG, "AlertManager shut down")
    }

    /**
     * Alert types for rate limiting
     */
    enum class AlertType {
        DROWSINESS,
        HAZARD,
        TRAFFIC_SIGN,
        GENERAL
    }

    /**
     * Alert priority
     */
    enum class Priority {
        NORMAL,
        CRITICAL
    }
}
