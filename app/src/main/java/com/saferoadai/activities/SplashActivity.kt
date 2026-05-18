package com.saferoadai.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.saferoadai.R

/**
 * Splash screen with Lottie animation
 * TODO: Add anim_splash.json to res/raw/
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        val animationView = findViewById<LottieAnimationView>(R.id.splashAnimation)
        animationView.setAnimation(R.raw.anim_logo)
        animationView.playAnimation()
        
        // Navigate to Auth after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }, 3000)
    }
}
