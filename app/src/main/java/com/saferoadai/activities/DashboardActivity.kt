package com.saferoadai.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.saferoadai.R
import com.saferoadai.db.AppDatabase
import com.saferoadai.location.LocationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Dashboard with Google Maps showing hazard markers and live location
 */
class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    private lateinit var googleMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        locationManager = LocationManager(this)
        database = AppDatabase.getDatabase(this)
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
        
        // Get last location and move camera
        locationManager.getLastLocation { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                Toast.makeText(this, "📍 Location found", Toast.LENGTH_SHORT).show()
            } else {
                // Default location (San Francisco)
                val defaultLocation = LatLng(37.7749, -122.4194)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
                Toast.makeText(this, "⚠️ Using default location", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Load hazards from database and display as red markers
        loadHazardMarkers()
    }
    
    /**
     * Load all hazards from database and display as red markers on map
     */
    private fun loadHazardMarkers() {
        lifecycleScope.launch {
            database.eventDao().getAllHazards().collectLatest { hazards ->
                // Clear existing markers (except user location)
                googleMap.clear()
                
                // Add red markers for each hazard
                hazards.forEach { hazard ->
                    val position = LatLng(hazard.latitude, hazard.longitude)
                    
                    // Red marker for potholes/hazards
                    val markerOptions = MarkerOptions()
                        .position(position)
                        .title(hazard.type)
                        .snippet("Severity: ${hazard.severity} | Confidence: ${(hazard.confidence * 100).toInt()}%")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    
                    googleMap.addMarker(markerOptions)
                }
                
                Toast.makeText(
                    this@DashboardActivity,
                    "📍 Loaded ${hazards.size} hazard markers",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun enableMyLocation() {
        try {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            Toast.makeText(this, "✅ Live location enabled", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "❌ Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission required for live location", Toast.LENGTH_LONG).show()
            }
        }
    }
}
