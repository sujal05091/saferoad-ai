package com.saferoadai.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.saferoadai.R

/**
 * Main home screen with navigation options
 */
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        auth = FirebaseAuth.getInstance()
        
        // Setup toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Display user email
        val userEmailText = findViewById<TextView>(R.id.userEmailText)
        auth.currentUser?.email?.let {
            userEmailText.text = it
        }
        
        // Logout button
        findViewById<MaterialButton>(R.id.logoutButton).setOnClickListener {
            showLogoutDialog()
        }
        
        setupNavigation()
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun setupNavigation() {
        findViewById<MaterialCardView>(R.id.startRideCard).setOnClickListener {
            startActivity(Intent(this, RideActivity::class.java))
        }
        
        findViewById<MaterialCardView>(R.id.dashboardCard).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        
        findViewById<MaterialCardView>(R.id.historyCard).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        findViewById<MaterialCardView>(R.id.settingsCard).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
