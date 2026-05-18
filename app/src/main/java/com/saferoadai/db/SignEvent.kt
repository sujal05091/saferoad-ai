package com.saferoadai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for traffic sign detection events
 */
@Entity(tableName = "sign_events")
data class SignEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val type: String,  // "Stop Sign", "Speed Limit 50", etc.
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val imageUri: String? = null,
    val confidence: Float,
    val importance: Int,  // Priority level (1-10)
    val uploadedToCloud: Boolean = false,
    val cloudId: String? = null
)
