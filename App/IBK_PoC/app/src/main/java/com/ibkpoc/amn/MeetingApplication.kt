// MeetingApplication.kt
package com.ibkpoc.amn

import android.app.Application
import com.ibkpoc.amn.util.Logger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MeetingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Logger.e("앱 크래시 발생", e)
        }
    }
}

