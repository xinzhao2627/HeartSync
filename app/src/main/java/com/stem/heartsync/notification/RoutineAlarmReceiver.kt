package com.stem.heartsync.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stem.heartsync.data.local.HeartSyncDbHelper

class RoutineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra("EXTRA_ROUTINE_ID", 0L)
        val routineTitle = intent.getStringExtra("EXTRA_ROUTINE_TITLE") ?: "Scheduled Routine"
        val timeOfDayStr = intent.getStringExtra("EXTRA_ROUTINE_TIME_OF_DAY") ?: "Daily"

        val dbHelper = HeartSyncDbHelper.getInstance(context)
        val prefs = dbHelper.getUserPreferences()

        if (!prefs.routineAlarmsEnabled) return

        val notificationManager = HeartSyncNotificationManager.getInstance(context)
        notificationManager.showRoutineReminder(routineTitle, timeOfDayStr, prefs)

        // Reschedule for the next day if routine exists and is active
        if (routineId > 0) {
            val routine = dbHelper.getAllRoutines().firstOrNull { it.id == routineId }
            if (routine != null && routine.isActive) {
                notificationManager.scheduleRoutineAlarm(routine)
            }
        }
    }
}
