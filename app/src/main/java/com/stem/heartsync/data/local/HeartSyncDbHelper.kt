package com.stem.heartsync.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.stem.heartsync.data.model.DailyJournalEntry
import com.stem.heartsync.data.model.DoctorProfile
import com.stem.heartsync.data.model.Medication
import com.stem.heartsync.data.model.MedicationLog
import com.stem.heartsync.data.model.NutritionGuideline
import com.stem.heartsync.data.model.NutritionType
import com.stem.heartsync.data.model.PatientProfile
import com.stem.heartsync.data.model.QuoteOrVerse
import com.stem.heartsync.data.model.RoutineItem
import com.stem.heartsync.data.model.RoutineLog
import com.stem.heartsync.data.model.RoutineTimeOfDay
import com.stem.heartsync.data.model.SavingsGoal
import com.stem.heartsync.data.model.SavingsTransaction
import com.stem.heartsync.data.model.UserPreferences
import com.stem.heartsync.data.model.UserRole

class HeartSyncDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "heartsync_offline.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: HeartSyncDbHelper? = null

        fun getInstance(context: Context): HeartSyncDbHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeartSyncDbHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        ensureSavingsColumns(writableDatabase)
    }

    private fun ensureSavingsColumns(db: SQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN target_date TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN quick_add_1 REAL DEFAULT 20.0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN quick_add_2 REAL DEFAULT 50.0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN quick_add_3 REAL DEFAULT 100.0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE savings_goals ADD COLUMN created_at INTEGER DEFAULT 0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE savings_transactions ADD COLUMN goal_name TEXT DEFAULT ''")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE user_preferences ADD COLUMN is_onboarding_completed INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE user_preferences ADD COLUMN routine_alarms_enabled INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE user_preferences ADD COLUMN medication_alarms_enabled INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE user_preferences (
                id INTEGER PRIMARY KEY,
                active_role TEXT NOT NULL,
                class_mode_enabled INTEGER NOT NULL DEFAULT 0,
                custom_ringtone_uri TEXT,
                sfx_enabled INTEGER NOT NULL DEFAULT 1,
                last_doctor_visit_timestamp INTEGER,
                is_onboarding_completed INTEGER NOT NULL DEFAULT 0,
                routine_alarms_enabled INTEGER NOT NULL DEFAULT 1,
                medication_alarms_enabled INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE doctor_profile (
                id INTEGER PRIMARY KEY,
                doctor_name TEXT NOT NULL,
                specialty TEXT NOT NULL,
                clinic_name TEXT NOT NULL,
                contact_phone TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE patient_profile (
                id INTEGER PRIMARY KEY,
                patient_name TEXT NOT NULL,
                age INTEGER NOT NULL,
                blood_type TEXT NOT NULL,
                condition_notes TEXT NOT NULL,
                emergency_contact_name TEXT NOT NULL,
                emergency_contact_phone TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE routine_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                time_of_day TEXT NOT NULL,
                target_time TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE routine_completion_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                routine_id INTEGER NOT NULL,
                date_key TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                completed_at INTEGER,
                UNIQUE(routine_id, date_key)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE medications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                dosage TEXT NOT NULL,
                frequency TEXT NOT NULL,
                scheduled_times TEXT NOT NULL,
                notes TEXT,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE medication_intake_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                medication_id INTEGER NOT NULL,
                scheduled_time TEXT NOT NULL,
                date_key TEXT NOT NULL,
                is_taken INTEGER NOT NULL DEFAULT 0,
                taken_at INTEGER,
                UNIQUE(medication_id, scheduled_time, date_key)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE nutrition_guidelines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                active_date TEXT NOT NULL,
                doctor_notes TEXT,
                is_active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE daily_journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_date TEXT NOT NULL UNIQUE,
                mood_score INTEGER NOT NULL DEFAULT 3,
                struggles TEXT,
                thankful_for TEXT,
                rich_text_content TEXT,
                heart_rate INTEGER,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_name TEXT NOT NULL,
                target_amount REAL NOT NULL,
                current_amount REAL NOT NULL,
                currency_symbol TEXT NOT NULL,
                target_date TEXT,
                quick_add_1 REAL DEFAULT 20.0,
                quick_add_2 REAL DEFAULT 50.0,
                quick_add_3 REAL DEFAULT 100.0,
                created_at INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE savings_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                goal_id INTEGER NOT NULL,
                goal_name TEXT DEFAULT '',
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                note TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE quotes_and_verses (
                id INTEGER PRIMARY KEY,
                day_of_year INTEGER NOT NULL UNIQUE,
                quote_text TEXT NOT NULL,
                reference_author TEXT NOT NULL,
                category TEXT NOT NULL,
                is_favorite INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Single release version; migrations can be appended here if schema evolves
    }

    // --- Preferences ---
    fun getUserPreferences(): UserPreferences {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM user_preferences WHERE id = 1", null)
        var prefs = UserPreferences()
        if (cursor.moveToFirst()) {
            val roleStr = cursor.getString(cursor.getColumnIndexOrThrow("active_role"))
            val classMode = cursor.getInt(cursor.getColumnIndexOrThrow("class_mode_enabled")) == 1
            val ringtone = cursor.getString(cursor.getColumnIndexOrThrow("custom_ringtone_uri"))
            val sfx = cursor.getInt(cursor.getColumnIndexOrThrow("sfx_enabled")) == 1
            val lastDoctorVisit = if (cursor.isNull(cursor.getColumnIndexOrThrow("last_doctor_visit_timestamp"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("last_doctor_visit_timestamp"))
            val onboardingIdx = cursor.getColumnIndex("is_onboarding_completed")
            val isOnboarding = if (onboardingIdx != -1 && !cursor.isNull(onboardingIdx)) {
                cursor.getInt(onboardingIdx) == 1
            } else false
            val routineIdx = cursor.getColumnIndex("routine_alarms_enabled")
            val routineAlarms = if (routineIdx != -1 && !cursor.isNull(routineIdx)) {
                cursor.getInt(routineIdx) == 1
            } else true
            val medIdx = cursor.getColumnIndex("medication_alarms_enabled")
            val medAlarms = if (medIdx != -1 && !cursor.isNull(medIdx)) {
                cursor.getInt(medIdx) == 1
            } else true
            val updated = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            prefs = UserPreferences(
                id = 1L,
                activeRole = if (roleStr == "DOCTOR") UserRole.DOCTOR else UserRole.PATIENT,
                classModeEnabled = classMode,
                customRingtoneUri = ringtone,
                sfxEnabled = sfx,
                lastDoctorVisitTimestamp = lastDoctorVisit,
                isOnboardingCompleted = isOnboarding,
                routineAlarmsEnabled = routineAlarms,
                medicationAlarmsEnabled = medAlarms,
                updatedAt = updated
            )
        }
        cursor.close()
        return prefs
    }

    fun saveUserPreferences(prefs: UserPreferences) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", 1L)
            put("active_role", prefs.activeRole.name)
            put("class_mode_enabled", if (prefs.classModeEnabled) 1 else 0)
            put("custom_ringtone_uri", prefs.customRingtoneUri)
            put("sfx_enabled", if (prefs.sfxEnabled) 1 else 0)
            put("last_doctor_visit_timestamp", prefs.lastDoctorVisitTimestamp)
            put("is_onboarding_completed", if (prefs.isOnboardingCompleted) 1 else 0)
            put("routine_alarms_enabled", if (prefs.routineAlarmsEnabled) 1 else 0)
            put("medication_alarms_enabled", if (prefs.medicationAlarmsEnabled) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("user_preferences", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Doctor Profile ---
    fun getDoctorProfile(): DoctorProfile {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM doctor_profile WHERE id = 1", null)
        var doctor = DoctorProfile()
        if (cursor.moveToFirst()) {
            doctor = DoctorProfile(
                id = 1L,
                doctorName = cursor.getString(cursor.getColumnIndexOrThrow("doctor_name")),
                specialty = cursor.getString(cursor.getColumnIndexOrThrow("specialty")),
                clinicName = cursor.getString(cursor.getColumnIndexOrThrow("clinic_name")),
                contactPhone = cursor.getString(cursor.getColumnIndexOrThrow("contact_phone")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
        }
        cursor.close()
        return doctor
    }

    fun saveDoctorProfile(doctor: DoctorProfile) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", 1L)
            put("doctor_name", doctor.doctorName)
            put("specialty", doctor.specialty)
            put("clinic_name", doctor.clinicName)
            put("contact_phone", doctor.contactPhone)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("doctor_profile", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Patient Profile ---
    fun getPatientProfile(): PatientProfile {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM patient_profile WHERE id = 1", null)
        var patient = PatientProfile()
        if (cursor.moveToFirst()) {
            patient = PatientProfile(
                id = 1L,
                patientName = cursor.getString(cursor.getColumnIndexOrThrow("patient_name")),
                age = cursor.getInt(cursor.getColumnIndexOrThrow("age")),
                bloodType = cursor.getString(cursor.getColumnIndexOrThrow("blood_type")),
                conditionNotes = cursor.getString(cursor.getColumnIndexOrThrow("condition_notes")),
                emergencyContactName = cursor.getString(cursor.getColumnIndexOrThrow("emergency_contact_name")),
                emergencyContactPhone = cursor.getString(cursor.getColumnIndexOrThrow("emergency_contact_phone")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
        }
        cursor.close()
        return patient
    }

    fun savePatientProfile(patient: PatientProfile) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", 1L)
            put("patient_name", patient.patientName)
            put("age", patient.age)
            put("blood_type", patient.bloodType)
            put("condition_notes", patient.conditionNotes)
            put("emergency_contact_name", patient.emergencyContactName)
            put("emergency_contact_phone", patient.emergencyContactPhone)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("patient_profile", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Routines ---
    fun getAllRoutines(): List<RoutineItem> {
        val list = mutableListOf<RoutineItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM routine_items WHERE is_active = 1 ORDER BY sort_order ASC, target_time ASC", null)
        while (cursor.moveToNext()) {
            val timeOfDayStr = cursor.getString(cursor.getColumnIndexOrThrow("time_of_day"))
            list.add(
                RoutineItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    timeOfDay = RoutineTimeOfDay.valueOf(timeOfDayStr),
                    targetTime = cursor.getString(cursor.getColumnIndexOrThrow("target_time")),
                    sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order")),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    fun upsertRoutine(item: RoutineItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (item.id > 0) put("id", item.id)
            put("title", item.title)
            put("time_of_day", item.timeOfDay.name)
            put("target_time", item.targetTime)
            put("sort_order", item.sortOrder)
            put("is_active", if (item.isActive) 1 else 0)
        }
        return db.insertWithOnConflict("routine_items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteRoutine(id: Long) {
        val db = writableDatabase
        db.delete("routine_items", "id = ?", arrayOf(id.toString()))
    }

    // Routine Logs
    fun getRoutineLogsForDate(dateKey: String): Map<Long, Boolean> {
        val map = mutableMapOf<Long, Boolean>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT routine_id, is_completed FROM routine_completion_logs WHERE date_key = ?", arrayOf(dateKey))
        while (cursor.moveToNext()) {
            val routineId = cursor.getLong(0)
            val isCompleted = cursor.getInt(1) == 1
            map[routineId] = isCompleted
        }
        cursor.close()
        return map
    }

    fun toggleRoutineCompletion(routineId: Long, dateKey: String, isCompleted: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("routine_id", routineId)
            put("date_key", dateKey)
            put("is_completed", if (isCompleted) 1 else 0)
            put("completed_at", if (isCompleted) System.currentTimeMillis() else null)
        }
        db.insertWithOnConflict("routine_completion_logs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Medications ---
    fun getAllMedications(): List<Medication> {
        val list = mutableListOf<Medication>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM medications WHERE is_active = 1 ORDER BY name ASC", null)
        while (cursor.moveToNext()) {
            val timesCsv = cursor.getString(cursor.getColumnIndexOrThrow("scheduled_times"))
            val timesList = if (timesCsv.isNullOrEmpty()) emptyList() else timesCsv.split(",").map { it.trim() }
            list.add(
                Medication(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    dosage = cursor.getString(cursor.getColumnIndexOrThrow("dosage")),
                    frequency = cursor.getString(cursor.getColumnIndexOrThrow("frequency")),
                    scheduledTimes = timesList,
                    notes = cursor.getString(cursor.getColumnIndexOrThrow("notes")) ?: "",
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    fun upsertMedication(med: Medication): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (med.id > 0) put("id", med.id)
            put("name", med.name)
            put("dosage", med.dosage)
            put("frequency", med.frequency)
            put("scheduled_times", med.scheduledTimes.joinToString(","))
            put("notes", med.notes)
            put("is_active", if (med.isActive) 1 else 0)
        }
        return db.insertWithOnConflict("medications", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteMedication(id: Long) {
        val db = writableDatabase
        db.delete("medications", "id = ?", arrayOf(id.toString()))
    }

    // Medication Logs
    fun getMedicationLogsForDate(dateKey: String): Map<String, Boolean> {
        // Key format: "medId_scheduledTime"
        val map = mutableMapOf<String, Boolean>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT medication_id, scheduled_time, is_taken FROM medication_intake_logs WHERE date_key = ?", arrayOf(dateKey))
        while (cursor.moveToNext()) {
            val medId = cursor.getLong(0)
            val time = cursor.getString(1)
            val isTaken = cursor.getInt(2) == 1
            map["${medId}_$time"] = isTaken
        }
        cursor.close()
        return map
    }

    fun toggleMedicationIntake(medicationId: Long, scheduledTime: String, dateKey: String, isTaken: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("medication_id", medicationId)
            put("scheduled_time", scheduledTime)
            put("date_key", dateKey)
            put("is_taken", if (isTaken) 1 else 0)
            put("taken_at", if (isTaken) System.currentTimeMillis() else null)
        }
        db.insertWithOnConflict("medication_intake_logs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Nutrition Guidelines ---
    fun getAllNutritionGuidelines(): List<NutritionGuideline> {
        val list = mutableListOf<NutritionGuideline>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM nutrition_guidelines WHERE is_active = 1 ORDER BY type ASC, category ASC", null)
        while (cursor.moveToNext()) {
            val typeStr = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            list.add(
                NutritionGuideline(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    type = NutritionType.valueOf(typeStr),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    activeDate = cursor.getString(cursor.getColumnIndexOrThrow("active_date")),
                    doctorNotes = cursor.getString(cursor.getColumnIndexOrThrow("doctor_notes")) ?: "",
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    fun upsertNutritionGuideline(item: NutritionGuideline): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (item.id > 0) put("id", item.id)
            put("type", item.type.name)
            put("description", item.description)
            put("category", item.category)
            put("active_date", item.activeDate)
            put("doctor_notes", item.doctorNotes)
            put("is_active", if (item.isActive) 1 else 0)
        }
        return db.insertWithOnConflict("nutrition_guidelines", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteNutritionGuideline(id: Long) {
        val db = writableDatabase
        db.delete("nutrition_guidelines", "id = ?", arrayOf(id.toString()))
    }

    // --- Daily Journal ---
    fun getJournalEntry(dateKey: String): DailyJournalEntry? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM daily_journal_entries WHERE entry_date = ?", arrayOf(dateKey))
        var entry: DailyJournalEntry? = null
        if (cursor.moveToFirst()) {
            entry = DailyJournalEntry(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                entryDate = cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                moodScore = cursor.getInt(cursor.getColumnIndexOrThrow("mood_score")),
                struggles = cursor.getString(cursor.getColumnIndexOrThrow("struggles")) ?: "",
                thankfulFor = cursor.getString(cursor.getColumnIndexOrThrow("thankful_for")) ?: "",
                richTextContent = cursor.getString(cursor.getColumnIndexOrThrow("rich_text_content")) ?: "",
                heartRate = if (cursor.isNull(cursor.getColumnIndexOrThrow("heart_rate"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("heart_rate")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
            )
        }
        cursor.close()
        return entry
    }

    fun getAllJournalEntries(): List<DailyJournalEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM daily_journal_entries ORDER BY entry_date DESC", null)
        val list = mutableListOf<DailyJournalEntry>()
        while (cursor.moveToNext()) {
            list.add(
                DailyJournalEntry(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    entryDate = cursor.getString(cursor.getColumnIndexOrThrow("entry_date")),
                    moodScore = cursor.getInt(cursor.getColumnIndexOrThrow("mood_score")),
                    struggles = cursor.getString(cursor.getColumnIndexOrThrow("struggles")) ?: "",
                    thankfulFor = cursor.getString(cursor.getColumnIndexOrThrow("thankful_for")) ?: "",
                    richTextContent = cursor.getString(cursor.getColumnIndexOrThrow("rich_text_content")) ?: "",
                    heartRate = if (cursor.isNull(cursor.getColumnIndexOrThrow("heart_rate"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("heart_rate")),
                    updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveJournalEntry(entry: DailyJournalEntry): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (entry.id > 0) put("id", entry.id)
            put("entry_date", entry.entryDate)
            put("mood_score", entry.moodScore)
            put("struggles", entry.struggles)
            put("thankful_for", entry.thankfulFor)
            put("rich_text_content", entry.richTextContent)
            put("heart_rate", entry.heartRate)
            put("updated_at", System.currentTimeMillis())
        }
        return db.insertWithOnConflict("daily_journal_entries", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Savings Goal & Transactions ---
    fun hasSavingsGoal(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM savings_goals WHERE id = 1", null)
        var has = false
        if (cursor.moveToFirst()) {
            has = cursor.getInt(0) > 0
        }
        cursor.close()
        return has
    }

    fun getAllSavingsGoals(): List<SavingsGoal> {
        val list = mutableListOf<SavingsGoal>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM savings_goals ORDER BY id ASC", null)
        while (cursor.moveToNext()) {
            val rawName = cursor.getString(cursor.getColumnIndexOrThrow("goal_name"))
            val cleanName = if (rawName.contains("Buffer", ignoreCase = true)) "Emergency Medicine Fund" else rawName
            val targetDateIdx = cursor.getColumnIndex("target_date")
            val targetDate = if (targetDateIdx != -1 && !cursor.isNull(targetDateIdx)) cursor.getString(targetDateIdx) else null

            val q1Idx = cursor.getColumnIndex("quick_add_1")
            val q1 = if (q1Idx != -1 && !cursor.isNull(q1Idx)) cursor.getDouble(q1Idx) else 20.0

            val q2Idx = cursor.getColumnIndex("quick_add_2")
            val q2 = if (q2Idx != -1 && !cursor.isNull(q2Idx)) cursor.getDouble(q2Idx) else 50.0

            val q3Idx = cursor.getColumnIndex("quick_add_3")
            val q3 = if (q3Idx != -1 && !cursor.isNull(q3Idx)) cursor.getDouble(q3Idx) else 100.0

            val createdIdx = cursor.getColumnIndex("created_at")
            val createdAt = if (createdIdx != -1 && !cursor.isNull(createdIdx)) cursor.getLong(createdIdx) else System.currentTimeMillis()

            list.add(
                SavingsGoal(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    goalName = cleanName,
                    targetAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("target_amount")),
                    currentAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("current_amount")),
                    currencySymbol = cursor.getString(cursor.getColumnIndexOrThrow("currency_symbol")),
                    targetDate = targetDate,
                    quickAdd1 = q1,
                    quickAdd2 = q2,
                    quickAdd3 = q3,
                    createdAt = createdAt
                )
            )
        }
        cursor.close()

        if (list.isEmpty()) {
            val defaultGoal = SavingsGoal(
                id = 1L,
                goalName = "Emergency Medicine Fund",
                targetAmount = 500.0,
                currentAmount = 0.0,
                currencySymbol = "₱"
            )
            insertOrUpdateSavingsGoal(defaultGoal)
            list.add(defaultGoal)
        }

        return list
    }

    fun getSavingsGoalById(id: Long): SavingsGoal? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM savings_goals WHERE id = ?", arrayOf(id.toString()))
        var item: SavingsGoal? = null
        if (cursor.moveToFirst()) {
            val rawName = cursor.getString(cursor.getColumnIndexOrThrow("goal_name"))
            val cleanName = if (rawName.contains("Buffer", ignoreCase = true)) "Emergency Medicine Fund" else rawName
            val targetDateIdx = cursor.getColumnIndex("target_date")
            val targetDate = if (targetDateIdx != -1 && !cursor.isNull(targetDateIdx)) cursor.getString(targetDateIdx) else null

            val q1Idx = cursor.getColumnIndex("quick_add_1")
            val q1 = if (q1Idx != -1 && !cursor.isNull(q1Idx)) cursor.getDouble(q1Idx) else 20.0

            val q2Idx = cursor.getColumnIndex("quick_add_2")
            val q2 = if (q2Idx != -1 && !cursor.isNull(q2Idx)) cursor.getDouble(q2Idx) else 50.0

            val q3Idx = cursor.getColumnIndex("quick_add_3")
            val q3 = if (q3Idx != -1 && !cursor.isNull(q3Idx)) cursor.getDouble(q3Idx) else 100.0

            val createdIdx = cursor.getColumnIndex("created_at")
            val createdAt = if (createdIdx != -1 && !cursor.isNull(createdIdx)) cursor.getLong(createdIdx) else System.currentTimeMillis()

            item = SavingsGoal(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                goalName = cleanName,
                targetAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("target_amount")),
                currentAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("current_amount")),
                currencySymbol = cursor.getString(cursor.getColumnIndexOrThrow("currency_symbol")),
                targetDate = targetDate,
                quickAdd1 = q1,
                quickAdd2 = q2,
                quickAdd3 = q3,
                createdAt = createdAt
            )
        }
        cursor.close()
        return item
    }

    fun getSavingsGoal(): SavingsGoal {
        return getAllSavingsGoals().firstOrNull() ?: SavingsGoal()
    }

    fun insertOrUpdateSavingsGoal(goal: SavingsGoal): Long {
        val db = writableDatabase
        val cleanName = if (goal.goalName.contains("Buffer", ignoreCase = true)) "Emergency Medicine Fund" else goal.goalName
        val values = ContentValues().apply {
            if (goal.id > 0L) {
                put("id", goal.id)
            }
            put("goal_name", cleanName)
            put("target_amount", if (goal.targetAmount <= 0.0) 500.0 else goal.targetAmount)
            put("current_amount", goal.currentAmount)
            put("currency_symbol", goal.currencySymbol.ifEmpty { "₱" })
            put("target_date", goal.targetDate)
            put("quick_add_1", goal.quickAdd1)
            put("quick_add_2", goal.quickAdd2)
            put("quick_add_3", goal.quickAdd3)
            put("created_at", if (goal.createdAt > 0) goal.createdAt else System.currentTimeMillis())
        }
        return db.insertWithOnConflict("savings_goals", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        insertOrUpdateSavingsGoal(goal)
    }

    fun deleteSavingsGoal(goalId: Long) {
        val db = writableDatabase
        db.delete("savings_goals", "id = ?", arrayOf(goalId.toString()))
        db.delete("savings_transactions", "goal_id = ?", arrayOf(goalId.toString()))
    }

    fun getSavingsTransactions(): List<SavingsTransaction> {
        val list = mutableListOf<SavingsTransaction>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM savings_transactions ORDER BY timestamp DESC", null)
        while (cursor.moveToNext()) {
            val goalNameIdx = cursor.getColumnIndex("goal_name")
            val goalName = if (goalNameIdx != -1 && !cursor.isNull(goalNameIdx)) cursor.getString(goalNameIdx) else ""

            list.add(
                SavingsTransaction(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    goalId = cursor.getLong(cursor.getColumnIndexOrThrow("goal_id")),
                    goalName = goalName,
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    note = cursor.getString(cursor.getColumnIndexOrThrow("note")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun applySavingsDelta(goalId: Long, amountDelta: Double, note: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val goal = getSavingsGoalById(goalId) ?: getAllSavingsGoals().first()
            val newAmount = (goal.currentAmount + amountDelta).coerceAtLeast(0.0)
            val valuesGoal = ContentValues().apply {
                put("id", goal.id)
                put("goal_name", goal.goalName)
                put("target_amount", if (goal.targetAmount <= 0.0) 500.0 else goal.targetAmount)
                put("current_amount", newAmount)
                put("currency_symbol", goal.currencySymbol.ifEmpty { "₱" })
                put("target_date", goal.targetDate)
                put("quick_add_1", goal.quickAdd1)
                put("quick_add_2", goal.quickAdd2)
                put("quick_add_3", goal.quickAdd3)
                put("created_at", goal.createdAt)
            }
            db.insertWithOnConflict("savings_goals", null, valuesGoal, SQLiteDatabase.CONFLICT_REPLACE)

            val valuesTx = ContentValues().apply {
                put("goal_id", goal.id)
                put("goal_name", goal.goalName)
                put("amount", kotlin.math.abs(amountDelta))
                put("type", if (amountDelta >= 0) "ADD" else "SUBTRACT")
                put("note", note)
                put("timestamp", System.currentTimeMillis())
            }
            db.insert("savings_transactions", null, valuesTx)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun applySavingsDelta(amountDelta: Double, note: String) {
        val firstGoal = getAllSavingsGoals().first()
        applySavingsDelta(firstGoal.id, amountDelta, note)
    }

    // --- Quotes & Verses ---
    fun getQuoteForDayOfYear(dayOfYear: Int): QuoteOrVerse? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM quotes_and_verses WHERE day_of_year = ? LIMIT 1", arrayOf(dayOfYear.toString()))
        var item: QuoteOrVerse? = null
        if (cursor.moveToFirst()) {
            item = QuoteOrVerse(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                dayOfYear = cursor.getInt(cursor.getColumnIndexOrThrow("day_of_year")),
                quoteText = cursor.getString(cursor.getColumnIndexOrThrow("quote_text")),
                referenceAuthor = cursor.getString(cursor.getColumnIndexOrThrow("reference_author")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")) == 1
            )
        }
        cursor.close()
        return item
    }

    fun insertQuotesBatch(quotes: List<QuoteOrVerse>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (q in quotes) {
                val values = ContentValues().apply {
                    put("id", q.id)
                    put("day_of_year", q.dayOfYear)
                    put("quote_text", q.quoteText)
                    put("reference_author", q.referenceAuthor)
                    put("category", q.category)
                    put("is_favorite", if (q.isFavorite) 1 else 0)
                }
                db.insertWithOnConflict("quotes_and_verses", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun hasQuotes(): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM quotes_and_verses", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count > 0
    }
}
