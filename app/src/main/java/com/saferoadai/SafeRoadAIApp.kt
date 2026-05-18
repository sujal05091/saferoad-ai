package com.saferoadai

import android.app.Application
import com.saferoadai.utils.CloudinaryManager

class SafeRoadAIApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary
        CloudinaryManager.init(this)
    }
}
