package com.stem.heartsync.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stem.heartsync.MainActivity
import com.stem.heartsync.R
import com.stem.heartsync.data.model.Medication
import com.stem.heartsync.data.model.RoutineItem
import com.stem.heartsync.data.model.UserPreferences
import java.util.Calendar

class HeartSyncNotificationManager private constructor(private val context: Context) {

    companion object {
        const val CHANNEL_STANDARD_ID = "heartsync_standard_channel"
        const val CHANNEL_CLASS_MODE_ID = "heartsync_class_mode_channel"
        const val CHANNEL_DAILY_QUOTE_ID = "heartsync_daily_quote_channel"

        const val NOTIFICATION_ID_DOCTOR_VISIT = 1001
        const val NOTIFICATION_ID_MEDICATION = 2001
        const val NOTIFICATION_ID_ROUTINE = 2002
        const val NOTIFICATION_ID_DAILY_QUOTE = 3001

        val CLASS_MODE_VIBRATION_PATTERN = longArrayOf(0, 350, 150, 350, 150, 600)

        @Volatile
        private var INSTANCE: HeartSyncNotificationManager? = null

        fun getInstance(context: Context): HeartSyncNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeartSyncNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Standard Channel (Audible with Custom/Default Sound)
            val standardChannel = NotificationChannel(
                CHANNEL_STANDARD_ID,
                "HeartSync Medication & Care Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard audible alerts for medications, routines, and doctor updates."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }

            // 2. Class Mode Channel (Vibration-Only, Blackboard Mode)
            val classModeChannel = NotificationChannel(
                CHANNEL_CLASS_MODE_ID,
                "HeartSync Class Mode (Quiet Study)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Silent vibration-only reminders when attending classes or study sessions."
                setSound(null, null) // Explicitly silent
                enableVibration(true)
                vibrationPattern = CLASS_MODE_VIBRATION_PATTERN
            }

            // 3. Daily Quote Channel (Public visibility on Lock Screen)
            val quoteChannel = NotificationChannel(
                CHANNEL_DAILY_QUOTE_ID,
                "HeartSync Daily Quote & Encouragement",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 12:00 AM inspirational heart quotes and scripture displayed on your lock screen."
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(standardChannel)
            notificationManager.createNotificationChannel(classModeChannel)
            notificationManager.createNotificationChannel(quoteChannel)
        }
    }

    fun showDoctorVisitNotification(doctorName: String, prefs: UserPreferences) {
        val isClassMode = prefs.classModeEnabled
        val channelId = if (isClassMode) CHANNEL_CLASS_MODE_ID else CHANNEL_STANDARD_ID

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "HEALTH_STREAM")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DOCTOR_VISIT,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getAlarmSoundUri(prefs.customRingtoneUri)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Doctor $doctorName visited your heart!")
            .setContentText("Your healthcare provider updated your care plan. Want to check your progress?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Doctor $doctorName visited your heart! Your condition notes, medications, and routine instructions have been configured. Want to check your progress?")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isClassMode) {
            builder.setSound(null)
            builder.setVibrate(CLASS_MODE_VIBRATION_PATTERN)
        } else {
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        }

        notificationManager.notify(NOTIFICATION_ID_DOCTOR_VISIT, builder.build())
    }

    fun showMedicationReminder(medicationName: String, dosage: String, prefs: UserPreferences) {
        if (!prefs.medicationAlarmsEnabled) return

        val isClassMode = prefs.classModeEnabled
        val channelId = if (isClassMode) CHANNEL_CLASS_MODE_ID else CHANNEL_STANDARD_ID

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "ROUTINES_MEDS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MEDICATION,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getAlarmSoundUri(prefs.customRingtoneUri)

        val title = if (isClassMode) "Scheduled Care Check-In" else "Medication Time: $medicationName"
        val body = if (isClassMode) "Time for your scheduled regimen ($dosage)" else "Take $medicationName ($dosage) as instructed by your healthcare provider."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isClassMode) {
            builder.setSound(null)
            builder.setVibrate(CLASS_MODE_VIBRATION_PATTERN)
        } else {
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        notificationManager.notify(NOTIFICATION_ID_MEDICATION, builder.build())
    }

    fun showRoutineReminder(routineTitle: String, timeOfDay: String, prefs: UserPreferences) {
        if (!prefs.routineAlarmsEnabled) return

        val isClassMode = prefs.classModeEnabled
        val channelId = if (isClassMode) CHANNEL_CLASS_MODE_ID else CHANNEL_STANDARD_ID

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "ROUTINES_MEDS")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_ROUTINE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getAlarmSoundUri(prefs.customRingtoneUri)

        val title = if (isClassMode) "Scheduled Routine Check-In" else "Routine Time: $routineTitle"
        val body = if (isClassMode) "Time for your $timeOfDay routine check" else "Time to complete your $timeOfDay routine: $routineTitle"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isClassMode) {
            builder.setSound(null)
            builder.setVibrate(CLASS_MODE_VIBRATION_PATTERN)
        } else {
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        }

        notificationManager.notify(NOTIFICATION_ID_ROUTINE, builder.build())
    }

    fun scheduleAllMedications(medications: List<Medication>, prefs: UserPreferences) {
        if (!prefs.medicationAlarmsEnabled) {
            cancelAllMedications(medications)
            return
        }
        for (med in medications) {
            if (med.isActive) {
                for (time in med.scheduledTimes) {
                    scheduleMedicationAlarm(med, time)
                }
            }
        }
    }

    fun cancelAllMedications(medications: List<Medication>) {
        for (med in medications) {
            for (time in med.scheduledTimes) {
                cancelMedicationAlarm(med, time)
            }
        }
    }

    fun scheduleMedicationAlarm(medication: Medication, timeStr: String) {
        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val hour = parts[0].trim().toIntOrNull() ?: return
        val minute = parts[1].trim().take(2).toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("EXTRA_MED_ID", medication.id)
            putExtra("EXTRA_MED_NAME", medication.name)
            putExtra("EXTRA_MED_DOSAGE", medication.dosage)
            putExtra("EXTRA_MED_TIME", timeStr)
        }

        val requestCode = (medication.id * 1000 + hour * 60 + minute).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelMedicationAlarm(medication: Medication, timeStr: String) {
        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val hour = parts[0].trim().toIntOrNull() ?: return
        val minute = parts[1].trim().take(2).toIntOrNull() ?: return
        val requestCode = (medication.id * 1000 + hour * 60 + minute).toInt()
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleAllRoutines(routines: List<RoutineItem>, prefs: UserPreferences) {
        if (!prefs.routineAlarmsEnabled) {
            cancelAllRoutines(routines)
            return
        }
        for (routine in routines) {
            if (routine.isActive) {
                scheduleRoutineAlarm(routine)
            }
        }
    }

    fun cancelAllRoutines(routines: List<RoutineItem>) {
        for (routine in routines) {
            cancelRoutineAlarm(routine)
        }
    }

    fun scheduleRoutineAlarm(routine: RoutineItem) {
        val parts = routine.targetTime.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            putExtra("EXTRA_ROUTINE_ID", routine.id)
            putExtra("EXTRA_ROUTINE_TITLE", routine.title)
            putExtra("EXTRA_ROUTINE_TIME_OF_DAY", routine.timeOfDay.name)
            putExtra("EXTRA_ROUTINE_TARGET_TIME", routine.targetTime)
        }

        val requestCode = (50000 + routine.id * 100 + hour * 60 + minute).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelRoutineAlarm(routine: RoutineItem) {
        val parts = routine.targetTime.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0
        val requestCode = (50000 + routine.id * 100 + hour * 60 + minute).toInt()
        val intent = Intent(context, RoutineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showDailyQuoteNotification(quoteText: String, author: String, prefs: UserPreferences) {
        val isClassMode = prefs.classModeEnabled
        val channelId = if (isClassMode) CHANNEL_CLASS_MODE_ID else CHANNEL_DAILY_QUOTE_ID

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "HOME")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY_QUOTE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Daily Heart Encouragement")
            .setContentText("\"$quoteText\" — $author")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Daily Heart Encouragement")
                    .bigText("\"$quoteText\"\n\n— $author")
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isClassMode) {
            builder.setSound(null)
            builder.setVibrate(CLASS_MODE_VIBRATION_PATTERN)
        }

        notificationManager.notify(NOTIFICATION_ID_DAILY_QUOTE, builder.build())
    }

    fun scheduleDailyMidnightQuoteAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, DailyQuoteAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_DAILY_QUOTE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun getAlarmSoundUri(customUriString: String?): Uri {
        if (!customUriString.isNullOrEmpty()) {
            try {
                val candidateUri = Uri.parse(customUriString)
                context.contentResolver.openInputStream(candidateUri)?.close()
                return candidateUri
            } catch (e: Exception) {
                // Fallback to default alarm sound
            }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }
}
