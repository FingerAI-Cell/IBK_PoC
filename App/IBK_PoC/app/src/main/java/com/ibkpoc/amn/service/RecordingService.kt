package com.ibkpoc.amn.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ibkpoc.amn.util.EventBus
import com.ibkpoc.amn.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class RecordingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        if (checkWakeLockPermission()) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "RecordingService::UploadWakeLock"
                )
            } catch (e: SecurityException) {
                Logger.e("WakeLock 권한 없음", e)
            }
        }
    }

    private fun checkWakeLockPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WAKE_LOCK
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkForegroundServicePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        try {
            if (checkWakeLockPermission()) {
                wakeLock?.acquire(10 * 60 * 1000L)
            }
            
            if (checkForegroundServicePermission() && checkNotificationPermission()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID, 
                            createNotification(), 
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, createNotification())
                    }
                } catch (e: SecurityException) {
                    Logger.e("Foreground 서비스 시작 실패", e)
                }
            }
            
            serviceScope.launch(Dispatchers.IO) {
                try {
                    EventBus.emitEvent(EventBus.RecordingEvent.ForceStop)
                } finally {
                    wakeLock?.release()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Logger.e("서비스 처리 중 오류", e)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "recording_service_channel"
        val channelName = "Recording Service"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (checkNotificationPermission()) {
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: SecurityException) {
                Logger.e("알림 채널 생성 실패", e)
            }
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("녹음 파일 저장 중")
            .setContentText("녹음 파일을 저장하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            wakeLock?.release()
        } catch (e: SecurityException) {
            Logger.e("WakeLock 해제 실패", e)
        }
        serviceScope.coroutineContext.cancelChildren()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1
    }
} 