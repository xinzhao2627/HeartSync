package com.stem.heartsync.data.repository

import android.content.Context
import com.stem.heartsync.data.local.DatabaseInitializer
import com.stem.heartsync.data.local.HeartSyncDbHelper
import com.stem.heartsync.data.model.DailyJournalEntry
import com.stem.heartsync.data.model.DoctorProfile
import com.stem.heartsync.data.model.Medication
import com.stem.heartsync.data.model.NutritionGuideline
import com.stem.heartsync.data.model.PatientProfile
import com.stem.heartsync.data.model.QuoteOrVerse
import com.stem.heartsync.data.model.RoutineItem
import com.stem.heartsync.data.model.SavingsGoal
import com.stem.heartsync.data.model.SavingsTransaction
import com.stem.heartsync.data.model.UserPreferences
import com.stem.heartsync.data.model.UserRole
import com.stem.heartsync.notification.HeartSyncNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HeartSyncRepository private constructor(private val context: Context) {

    private val dbHelper = HeartSyncDbHelper.getInstance(context)
    private val notificationManager = HeartSyncNotificationManager.getInstance(context)

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private val _doctorProfile = MutableStateFlow(DoctorProfile())
    val doctorProfile: StateFlow<DoctorProfile> = _doctorProfile.asStateFlow()

    private val _patientProfile = MutableFlowWrapper(PatientProfile())
    val patientProfile: StateFlow<PatientProfile> = _patientProfile.flow

    private val _routines = MutableStateFlow<List<RoutineItem>>(emptyList())
    val routines: StateFlow<List<RoutineItem>> = _routines.asStateFlow()

    private val _routineLogs = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val routineLogs: StateFlow<Map<Long, Boolean>> = _routineLogs.asStateFlow()

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    private val _medicationLogs = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val medicationLogs: StateFlow<Map<String, Boolean>> = _medicationLogs.asStateFlow()

    private val _nutritionGuidelines = MutableStateFlow<List<NutritionGuideline>>(emptyList())
    val nutritionGuidelines: StateFlow<List<NutritionGuideline>> = _nutritionGuidelines.asStateFlow()

    private val _savingsGoal = MutableStateFlow(SavingsGoal())
    val savingsGoal: StateFlow<SavingsGoal> = _savingsGoal.asStateFlow()

    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoal>> = _savingsGoals.asStateFlow()

    private val _savingsTransactions = MutableStateFlow<List<SavingsTransaction>>(emptyList())
    val savingsTransactions: StateFlow<List<SavingsTransaction>> = _savingsTransactions.asStateFlow()

    private val _journalEntries = MutableStateFlow<List<DailyJournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<DailyJournalEntry>> = _journalEntries.asStateFlow()

    private val _todaysQuote = MutableStateFlow<QuoteOrVerse?>(null)
    val todaysQuote: StateFlow<QuoteOrVerse?> = _todaysQuote.asStateFlow()

    private val _currentDateKey = MutableStateFlow(getTodayDateKey())
    val currentDateKey: StateFlow<String> = _currentDateKey.asStateFlow()

    init {
        DatabaseInitializer.initializeIfEmpty(context)
        refreshAll()
    }

    fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun refreshAll() {
        val today = getTodayDateKey()
        _currentDateKey.value = today
        _userPreferences.value = dbHelper.getUserPreferences()
        _doctorProfile.value = dbHelper.getDoctorProfile()
        _patientProfile.update(dbHelper.getPatientProfile())
        _routines.value = dbHelper.getAllRoutines()
        _routineLogs.value = dbHelper.getRoutineLogsForDate(today)
        _medications.value = dbHelper.getAllMedications()
        _medicationLogs.value = dbHelper.getMedicationLogsForDate(today)
        _nutritionGuidelines.value = dbHelper.getAllNutritionGuidelines()
        val allGoals = dbHelper.getAllSavingsGoals()
        _savingsGoals.value = allGoals
        _savingsGoal.value = allGoals.firstOrNull() ?: SavingsGoal()
        _savingsTransactions.value = dbHelper.getSavingsTransactions()
        _journalEntries.value = dbHelper.getAllJournalEntries()

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        _todaysQuote.value = dbHelper.getQuoteForDayOfYear(dayOfYear)

        notificationManager.scheduleAllMedications(_medications.value, _userPreferences.value)
        notificationManager.scheduleAllRoutines(_routines.value, _userPreferences.value)
    }

    // Role Switcher (Pure KISS: instant toggle)
    fun switchRole(newRole: UserRole) {
        val current = _userPreferences.value
        val updated = current.copy(activeRole = newRole, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
    }

    fun toggleClassMode() {
        val current = _userPreferences.value
        val newClassMode = !current.classModeEnabled
        val updated = current.copy(classModeEnabled = newClassMode, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
    }

    fun setCustomRingtoneUri(uriString: String?) {
        val current = _userPreferences.value
        val updated = current.copy(customRingtoneUri = uriString, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
    }

    fun toggleSfx(enabled: Boolean) {
        val current = _userPreferences.value
        val updated = current.copy(sfxEnabled = enabled, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
    }

    fun toggleRoutineAlarms(enabled: Boolean) {
        val current = _userPreferences.value
        val updated = current.copy(routineAlarmsEnabled = enabled, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
        notificationManager.scheduleAllRoutines(_routines.value, updated)
    }

    fun toggleMedicationAlarms(enabled: Boolean) {
        val current = _userPreferences.value
        val updated = current.copy(medicationAlarmsEnabled = enabled, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
        notificationManager.scheduleAllMedications(_medications.value, updated)
    }

    fun toggleWeatherAlarms(enabled: Boolean) {
        val current = _userPreferences.value
        val updated = current.copy(weatherAlarmsEnabled = enabled, updatedAt = System.currentTimeMillis())
        dbHelper.saveUserPreferences(updated)
        _userPreferences.value = updated
        if (enabled) {
            notificationManager.schedulePeriodicWeatherCheckAlarm()
        } else {
            notificationManager.cancelPeriodicWeatherCheckAlarm()
        }
    }

    fun completeOnboarding(patient: PatientProfile, doctor: DoctorProfile) {
        dbHelper.savePatientProfile(patient)
        dbHelper.saveDoctorProfile(doctor)
        _patientProfile.update(patient)
        _doctorProfile.value = doctor

        val currentPrefs = _userPreferences.value
        val updatedPrefs = currentPrefs.copy(
            isOnboardingCompleted = true,
            updatedAt = System.currentTimeMillis()
        )
        dbHelper.saveUserPreferences(updatedPrefs)
        _userPreferences.value = updatedPrefs
    }

    fun resetOnboarding() {
        val currentPrefs = _userPreferences.value
        val updatedPrefs = currentPrefs.copy(
            isOnboardingCompleted = false,
            updatedAt = System.currentTimeMillis()
        )
        dbHelper.saveUserPreferences(updatedPrefs)
        _userPreferences.value = updatedPrefs
    }

    // Doctor updates patient profile / condition
    fun updatePatientProfile(updated: PatientProfile, isDoctorAction: Boolean = false) {
        dbHelper.savePatientProfile(updated)
        _patientProfile.update(updated)
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
    }

    fun updateDoctorProfile(updated: DoctorProfile) {
        dbHelper.saveDoctorProfile(updated)
        _doctorProfile.value = updated
    }

    // Routines CRUD
    fun saveRoutine(item: RoutineItem, isDoctorAction: Boolean = false): Long {
        val id = dbHelper.upsertRoutine(item)
        _routines.value = dbHelper.getAllRoutines()
        notificationManager.scheduleAllRoutines(_routines.value, _userPreferences.value)
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
        return id
    }

    fun deleteRoutine(id: Long, isDoctorAction: Boolean = false) {
        dbHelper.deleteRoutine(id)
        _routines.value = dbHelper.getAllRoutines()
        notificationManager.scheduleAllRoutines(_routines.value, _userPreferences.value)
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
    }

    fun toggleRoutineCompleted(routineId: Long, isCompleted: Boolean) {
        val today = _currentDateKey.value
        dbHelper.toggleRoutineCompletion(routineId, today, isCompleted)
        _routineLogs.value = dbHelper.getRoutineLogsForDate(today)
    }

    // Medications CRUD
    fun saveMedication(med: Medication, isDoctorAction: Boolean = false): Long {
        val id = dbHelper.upsertMedication(med)
        _medications.value = dbHelper.getAllMedications()
        notificationManager.scheduleAllMedications(_medications.value, _userPreferences.value)
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
        return id
    }

    fun deleteMedication(id: Long, isDoctorAction: Boolean = false) {
        dbHelper.deleteMedication(id)
        _medications.value = dbHelper.getAllMedications()
        notificationManager.scheduleAllMedications(_medications.value, _userPreferences.value)
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
    }

    fun toggleMedicationTaken(medicationId: Long, scheduledTime: String, isTaken: Boolean) {
        val today = _currentDateKey.value
        dbHelper.toggleMedicationIntake(medicationId, scheduledTime, today, isTaken)
        _medicationLogs.value = dbHelper.getMedicationLogsForDate(today)
    }

    fun toggleMedicationIntake(medicationId: Long, scheduledTime: String, isTaken: Boolean) {
        toggleMedicationTaken(medicationId, scheduledTime, isTaken)
    }

    // Nutrition Guidelines CRUD
    fun saveNutritionGuideline(item: NutritionGuideline, isDoctorAction: Boolean = false): Long {
        val id = dbHelper.upsertNutritionGuideline(item)
        _nutritionGuidelines.value = dbHelper.getAllNutritionGuidelines()
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
        return id
    }

    fun deleteNutritionGuideline(id: Long, isDoctorAction: Boolean = false) {
        dbHelper.deleteNutritionGuideline(id)
        _nutritionGuidelines.value = dbHelper.getAllNutritionGuidelines()
        if (isDoctorAction) {
            recordDoctorVisitAndNotify()
        }
    }

    // Daily Journal
    fun getJournalForDate(dateKey: String): DailyJournalEntry? {
        return dbHelper.getJournalEntry(dateKey)
    }

    fun saveJournal(entry: DailyJournalEntry): Long {
        val id = dbHelper.saveJournalEntry(entry)
        refreshAll()
        return id
    }

    // Savings Goals
    fun refreshSavings() {
        val allGoals = dbHelper.getAllSavingsGoals()
        _savingsGoals.value = allGoals
        _savingsGoal.value = allGoals.firstOrNull() ?: SavingsGoal()
        _savingsTransactions.value = dbHelper.getSavingsTransactions()
    }

    fun saveSavingsGoal(goal: SavingsGoal): Long {
        val id = dbHelper.insertOrUpdateSavingsGoal(goal)
        refreshSavings()
        return id
    }

    fun deleteSavingsGoal(goalId: Long) {
        dbHelper.deleteSavingsGoal(goalId)
        refreshSavings()
    }

    fun adjustSavings(goalId: Long, amountDelta: Double, note: String) {
        dbHelper.applySavingsDelta(goalId, amountDelta, note)
        refreshSavings()
    }

    fun adjustSavings(amountDelta: Double, note: String) {
        val firstGoal = _savingsGoals.value.firstOrNull() ?: _savingsGoal.value
        adjustSavings(firstGoal.id, amountDelta, note)
    }

    fun updateSavingsGoalTarget(newTarget: Double) {
        val current = _savingsGoal.value
        val updated = current.copy(targetAmount = newTarget)
        saveSavingsGoal(updated)
    }

    fun updateSavingsGoal(newName: String, newTarget: Double) {
        val current = _savingsGoal.value
        val updated = current.copy(
            goalName = newName.ifBlank { "Emergency Medicine Fund" },
            targetAmount = newTarget
        )
        saveSavingsGoal(updated)
    }

    // Doctor Visit Trigger
    fun recordDoctorVisitAndNotify() {
        val timestamp = System.currentTimeMillis()
        val currentPrefs = _userPreferences.value
        val updatedPrefs = currentPrefs.copy(lastDoctorVisitTimestamp = timestamp)
        dbHelper.saveUserPreferences(updatedPrefs)
        _userPreferences.value = updatedPrefs

        val doctorName = _doctorProfile.value.doctorName.ifEmpty { "Maria Santos" }
        notificationManager.showDoctorVisitNotification(doctorName, updatedPrefs)
    }

    companion object {
        @Volatile
        private var INSTANCE: HeartSyncRepository? = null

        fun getInstance(context: Context): HeartSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeartSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private class MutableFlowWrapper<T>(initial: T) {
    private val _flow = MutableStateFlow(initial)
    val flow: StateFlow<T> = _flow.asStateFlow()
    fun update(value: T) {
        _flow.value = value
    }
}
