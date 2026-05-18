package com.saferoadai.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.*

/**
 * Location Manager using Google Play Services FusedLocationProviderClient
 * Provides real-time location updates and distance calculations
 */
class LocationManager(private val context: Context) {

    companion object {
        private const val TAG = "LocationManager"
        private const val UPDATE_INTERVAL = 5000L  // 5 seconds
        private const val FASTEST_INTERVAL = 2000L  // 2 seconds
        private const val MIN_DISTANCE_METERS = 10f  // Minimum distance for updates
        
        // Earth radius in meters
        private const val EARTH_RADIUS = 6371000.0
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val altitude: Double,
        val bearing: Float,
        val speed: Float,
        val timestamp: Long
    )

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get last known location (one-time)
     */
    fun getLastLocation(onLocation: (LocationData?) -> Unit) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            onLocation(null)
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                lastKnownLocation = location
                onLocation(location?.toLocationData())
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last location", e)
                onLocation(null)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
            onLocation(null)
        }
    }

    /**
     * Start continuous location updates as Flow
     */
    fun startLocationUpdates(): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastKnownLocation = location
                    trySend(location.toLocationData())
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Log.w(TAG, "Location not available")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
            close(e)
        }

        awaitClose {
            stopLocationUpdates()
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.i(TAG, "Location updates stopped")
        }
    }

    /**
     * Calculate distance between two locations in meters using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS * c
    }

    /**
     * Calculate distance from current location to a point
     */
    fun distanceFromCurrent(latitude: Double, longitude: Double): Double? {
        val current = lastKnownLocation ?: return null
        return calculateDistance(current.latitude, current.longitude, latitude, longitude)
    }

    /**
     * Check if two locations are within threshold distance
     */
    fun isNearby(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        thresholdMeters: Double = 50.0
    ): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= thresholdMeters
    }

    /**
     * Get current location (cached or fresh)
     */
    fun getCurrentLocation(): LocationData? {
        return lastKnownLocation?.toLocationData()
    }

    /**
     * Convert Android Location to LocationData
     */
    private fun Location.toLocationData(): LocationData {
        return LocationData(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = altitude,
            bearing = bearing,
            speed = speed,
            timestamp = time
        )
    }

    /**
     * Format coordinates for display
     */
    fun formatCoordinates(lat: Double, lon: Double): String {
        return String.format("%.6f, %.6f", lat, lon)
    }

    /**
     * Get bearing between two points (in degrees)
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
}
