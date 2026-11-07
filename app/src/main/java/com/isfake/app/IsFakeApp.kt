package com.isfake.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class IsFakeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize OSMDroid configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
