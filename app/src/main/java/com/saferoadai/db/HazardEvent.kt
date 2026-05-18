package com.saferoadai.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for road hazard events (potholes, debris, cracks, etc.)
 */
@Entity(tableName = "hazard_events")
data class HazardEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val type: String,  // "Pothole", "Crack", "Debris", etc.
    val latitude: Double,
    val longitude: Double,
    val timestampFirstSeen: Long,  // First detection timestamp
    val timestampLastSeen: Long,   // Last seen (for recurring hazards)
    val imageUri: String? = null,  // Optional: local image path
    val confidence: Float,
    val severity: String,  // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val status: String = "ACTIVE",  // "ACTIVE", "FIXED", "VERIFIED"
    val uploadedToCloud: Boolean = false,
    val cloudId: String? = null,  // Firebase document ID
    val notes: String? = null,
    val reportedBy: String? = null  // User ID
)
