package com.stem.heartsync.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stem.heartsync.data.local.HeartSyncDbHelper
import com.stem.heartsync.weather.WeatherManager

class WeatherAlertAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dbHelper = HeartSyncDbHelper.getInstance(context)
        val prefs = dbHelper.getUserPreferences()

        val notificationManager = HeartSyncNotificationManager.getInstance(context)

        // Only check and notify if weather alarms are enabled
        if (prefs.weatherAlarmsEnabled) {
            val weatherManager = WeatherManager.getInstance(context)
            weatherManager.checkWeatherForAlerts()
        }

        // Reschedule next background weather check
        notificationManager.schedulePeriodicWeatherCheckAlarm()
    }
}
