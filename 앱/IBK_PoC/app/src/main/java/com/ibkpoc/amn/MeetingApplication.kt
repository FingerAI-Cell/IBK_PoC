// MeetingApplication.kt
package com.ibkpoc.amn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.ibkpoc.amn.util.Logger

@HiltAndroidApp
class MeetingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 앱 크래시 처리
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e(
                "앱 비정상 종료", 
                throwable,
                applicationContext
            )
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }
}

