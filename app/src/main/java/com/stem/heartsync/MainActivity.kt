package com.stem.heartsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.HeartSyncApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = HeartSyncRepository.getInstance(applicationContext)
        val audioManager = HeartSyncAudioManager.getInstance(applicationContext)
        val notificationManager = com.stem.heartsync.notification.HeartSyncNotificationManager.getInstance(applicationContext)

        // Schedule daily midnight quote notification alarm
        notificationManager.scheduleDailyMidnightQuoteAlarm()

        // Schedule periodic extreme weather monitoring alarm
        notificationManager.schedulePeriodicWeatherCheckAlarm()

        setContent {
            HeartSyncApp(
                repository = repository,
                audioManager = audioManager
            )
        }
    }
}