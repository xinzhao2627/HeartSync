package com.stem.heartsync.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stem.heartsync.data.local.HeartSyncDbHelper
import java.util.Calendar

class DailyQuoteAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dbHelper = HeartSyncDbHelper.getInstance(context)
        val prefs = dbHelper.getUserPreferences()
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val quote = dbHelper.getQuoteForDayOfYear(dayOfYear)

        val notificationManager = HeartSyncNotificationManager.getInstance(context)
        if (quote != null) {
            notificationManager.showDailyQuoteNotification(
                quoteText = quote.quoteText,
                author = quote.referenceAuthor,
                prefs = prefs
            )
        }

        // Re-schedule for the next midnight
        notificationManager.scheduleDailyMidnightQuoteAlarm()
    }
}
