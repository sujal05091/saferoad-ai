package com.saferoadai.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for all event types
 */
@Dao
interface EventDao {
    
    // ===== Hazard Events =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHazard(hazard: HazardEvent): Long
    
    @Update
    suspend fun updateHazard(hazard: HazardEvent)
    
    @Query("SELECT * FROM hazard_events ORDER BY timestampFirstSeen DESC")
    fun getAllHazards(): Flow<List<HazardEvent>>
    
    @Query("SELECT * FROM hazard_events WHERE status = :status ORDER BY timestampFirstSeen DESC")
    fun getHazardsByStatus(status: String): Flow<List<HazardEvent>>
    
    @Query("SELECT * FROM hazard_events WHERE id = :id")
    suspend fun getHazardById(id: Long): HazardEvent?
    
    @Query("SELECT * FROM hazard_events WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    suspend fun getHazardsInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<HazardEvent>
    
    @Query("UPDATE hazard_events SET status = :status WHERE id = :id")
    suspend fun updateHazardStatus(id: Long, status: String)
    
    @Query("UPDATE hazard_events SET uploadedToCloud = :uploaded, cloudId = :cloudId WHERE id = :id")
    suspend fun markHazardUploaded(id: Long, uploaded: Boolean, cloudId: String?)
    
    @Delete
    suspend fun deleteHazard(hazard: HazardEvent)
    
    // ===== Sign Events =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSign(sign: SignEvent): Long
    
    @Query("SELECT * FROM sign_events ORDER BY timestamp DESC")
    fun getAllSigns(): Flow<List<SignEvent>>
    
    @Query("SELECT * FROM sign_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSigns(limit: Int = 100): Flow<List<SignEvent>>
    
    @Query("SELECT * FROM sign_events WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    suspend fun getSignsInBounds(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<SignEvent>
    
    @Delete
    suspend fun deleteSign(sign: SignEvent)
    
    @Query("DELETE FROM sign_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldSigns(cutoffTimestamp: Long)
    
    // ===== Drowsiness Events =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrowsiness(event: DrowsinessEvent): Long
    
    @Query("SELECT * FROM drowsiness_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDrowsinessEvents(limit: Int = 100): Flow<List<DrowsinessEvent>>
    
    @Query("SELECT * FROM drowsiness_events WHERE rideId = :rideId ORDER BY timestamp ASC")
    fun getDrowsinessEventsByRide(rideId: String): Flow<List<DrowsinessEvent>>
    
    @Query("SELECT * FROM drowsiness_events WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getDrowsinessEventsInTimeRange(startTime: Long, endTime: Long): List<DrowsinessEvent>
    
    @Delete
    suspend fun deleteDrowsiness(event: DrowsinessEvent)
    
    @Query("DELETE FROM drowsiness_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldDrowsinessEvents(cutoffTimestamp: Long)
    
    // ===== Statistics Queries =====
    @Query("SELECT COUNT(*) FROM hazard_events WHERE status = 'ACTIVE'")
    suspend fun getActiveHazardCount(): Int
    
    @Query("SELECT COUNT(*) FROM drowsiness_events WHERE level IN ('DROWSY', 'VERY_DROWSY')")
    suspend fun getDrowsinessAlertCount(): Int
    
    @Query("SELECT type, COUNT(*) as count FROM hazard_events GROUP BY type ORDER BY count DESC")
    suspend fun getHazardTypeCounts(): List<HazardTypeCount>
}

data class HazardTypeCount(
    val type: String,
    val count: Int
)
