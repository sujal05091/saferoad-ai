package com.saferoadai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for drowsiness detection events
 */
@Entity(tableName = "drowsiness_events")
data class DrowsinessEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val level: String,  // "ALERT", "DROWSY", "VERY_DROWSY", "YAWNING", "DISTRACTED"
    val earValue: Float,
    val marValue: Float,
    val headPoseYaw: Float,
    val headPosePitch: Float,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val message: String,
    val rideId: String? = null  // Associate with ride session
)
