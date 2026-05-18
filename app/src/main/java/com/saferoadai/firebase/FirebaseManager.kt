package com.saferoadai.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.saferoadai.db.HazardEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Manager for cloud sync and real-time updates
 * TODO: Add google-services.json to app/ folder
 */
class FirebaseManager {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val HAZARDS_COLLECTION = "hazards"
        private const val SIGNS_COLLECTION = "traffic_signs"
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Upload hazard event to Firestore
     * Returns document ID if successful
     */
    suspend fun uploadHazard(hazard: HazardEvent): String? {
        return try {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: "anonymous"

            val hazardData = hashMapOf(
                "type" to hazard.type,
                "latitude" to hazard.latitude,
                "longitude" to hazard.longitude,
                "timestampFirstSeen" to hazard.timestampFirstSeen,
                "timestampLastSeen" to hazard.timestampLastSeen,
                "confidence" to hazard.confidence,
                "severity" to hazard.severity,
                "status" to hazard.status,
                "reportedBy" to userId,
                "notes" to hazard.notes,
                "uploadedAt" to System.currentTimeMillis()
            )

            val documentRef = firestore.collection(HAZARDS_COLLECTION)
                .add(hazardData)
                .await()

            Log.i(TAG, "Hazard uploaded successfully: ${documentRef.id}")
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload hazard", e)
            null
        }
    }

    /**
     * Mark hazard as fixed in Firestore
     */
    suspend fun markHazardFixed(cloudId: String): Boolean {
        return try {
            firestore.collection(HAZARDS_COLLECTION)
                .document(cloudId)
                .update(
                    mapOf(
                        "status" to "FIXED",
                        "fixedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Log.i(TAG, "Hazard marked as fixed: $cloudId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark hazard as fixed", e)
            false
        }
    }

    /**
     * Stream active hazards from Firestore (real-time)
     * Returns Flow of hazard events
     */
    fun streamHazards(): Flow<List<HazardEvent>> = callbackFlow {
        val listener = firestore.collection(HAZARDS_COLLECTION)
            .whereEqualTo("status", "ACTIVE")
            .orderBy("timestampFirstSeen", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error streaming hazards", error)
                    return@addSnapshotListener
                }

                val hazards = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        HazardEvent(
                            id = 0,  // Local ID not needed for cloud data
                            type = doc.getString("type") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            timestampFirstSeen = doc.getLong("timestampFirstSeen") ?: 0L,
                            timestampLastSeen = doc.getLong("timestampLastSeen") ?: 0L,
                            confidence = doc.getDouble("confidence")?.toFloat() ?: 0f,
                            severity = doc.getString("severity") ?: "MEDIUM",
                            status = doc.getString("status") ?: "ACTIVE",
                            uploadedToCloud = true,
                            cloudId = doc.id,
                            notes = doc.getString("notes"),
                            reportedBy = doc.getString("reportedBy")
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse hazard document", e)
                        null
                    }
                } ?: emptyList()

                trySend(hazards)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get hazards within geographic bounds
     */
    suspend fun getHazardsInBounds(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double
    ): List<HazardEvent> {
        return try {
            val snapshot = firestore.collection(HAZARDS_COLLECTION)
                .whereGreaterThanOrEqualTo("latitude", minLat)
                .whereLessThanOrEqualTo("latitude", maxLat)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val lon = doc.getDouble("longitude") ?: return@mapNotNull null
                if (lon < minLon || lon > maxLon) return@mapNotNull null

                HazardEvent(
                    id = 0,
                    type = doc.getString("type") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = lon,
                    timestampFirstSeen = doc.getLong("timestampFirstSeen") ?: 0L,
                    timestampLastSeen = doc.getLong("timestampLastSeen") ?: 0L,
                    confidence = doc.getDouble("confidence")?.toFloat() ?: 0f,
                    severity = doc.getString("severity") ?: "MEDIUM",
                    status = doc.getString("status") ?: "ACTIVE",
                    uploadedToCloud = true,
                    cloudId = doc.id,
                    notes = doc.getString("notes"),
                    reportedBy = doc.getString("reportedBy")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get hazards in bounds", e)
            emptyList()
        }
    }

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }
}
