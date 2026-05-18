package com.saferoadai.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.saferoadai.R

/**
 * Settings activity for app configuration
 * TODO: Implement DataStore for preferences
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupSettings()
    }

    private fun setupSettings() {
        val audioSwitch = findViewById<SwitchMaterial>(R.id.audioSwitch)
        val uploadSwitch = findViewById<SwitchMaterial>(R.id.uploadSwitch)
        
        // TODO: Load saved preferences from DataStore
        // TODO: Save preferences on change
        
        audioSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
        }
        
        uploadSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
        }
    }
}
