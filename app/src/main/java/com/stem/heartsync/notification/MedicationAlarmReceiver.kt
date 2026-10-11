package com.stem.heartsync.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stem.heartsync.data.local.HeartSyncDbHelper

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra("EXTRA_MED_ID", 0L)
        val medName = intent.getStringExtra("EXTRA_MED_NAME") ?: "Prescribed Medication"
        val medDosage = intent.getStringExtra("EXTRA_MED_DOSAGE") ?: ""
        val medTime = intent.getStringExtra("EXTRA_MED_TIME") ?: ""

        val dbHelper = HeartSyncDbHelper.getInstance(context)
        val prefs = dbHelper.getUserPreferences()

        if (!prefs.medicationAlarmsEnabled) return

        val notificationManager = HeartSyncNotificationManager.getInstance(context)
        notificationManager.showMedicationReminder(medName, medDosage, prefs)

        // Reschedule for next day if medication exists and is active
        if (medId > 0 && medTime.isNotEmpty()) {
            val med = dbHelper.getAllMedications().firstOrNull { it.id == medId }
            if (med != null && med.isActive) {
                notificationManager.scheduleMedicationAlarm(med, medTime)
            }
        }
    }
}
