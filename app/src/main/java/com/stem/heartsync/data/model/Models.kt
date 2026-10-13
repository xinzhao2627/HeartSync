package com.stem.heartsync.data.model

enum class UserRole {
    PATIENT,
    DOCTOR
}

enum class PatientNavDestination(val label: String) {
    HOME("Home"),
    ROUTINES("Meds"),
    JOURNAL("Notes"),
    SAVINGS("Savings"),
    SETTINGS("Settings")
}

enum class RoutineTimeOfDay {
    MORNING,
    AFTERNOON,
    NIGHT
}

enum class NutritionType {
    TO_EAT,
    TO_AVOID
}

data class UserPreferences(
    val id: Long = 1L,
    val activeRole: UserRole = UserRole.PATIENT,
    val classModeEnabled: Boolean = false,
    val customRingtoneUri: String? = null,
    val sfxEnabled: Boolean = true,
    val lastDoctorVisitTimestamp: Long? = null,
    val isOnboardingCompleted: Boolean = false,
    val routineAlarmsEnabled: Boolean = true,
    val medicationAlarmsEnabled: Boolean = true,
    val weatherAlarmsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DoctorProfile(
    val id: Long = 1L,
    val doctorName: String = "Dr. Maria Santos, MD",
    val specialty: String = "Cardiology & Vascular Health",
    val clinicName: String = "Philippine Heart Center Clinic",
    val contactPhone: String = "+63 917 555 0199",
    val updatedAt: Long = System.currentTimeMillis()
)

data class PatientProfile(
    val id: Long = 1L,
    val patientName: String = "Juan Dela Cruz",
    val age: Int = 19,
    val bloodType: String = "O+",
    val conditionNotes: String = "Stage 1 Essential Hypertension & Episodic Arrhythmia. Monitor resting pulse, avoid caffeinated energy drinks, stay well hydrated.",
    val emergencyContactName: String = "Dr. Santos / School Clinic",
    val emergencyContactPhone: String = "+63 917 555 0199",
    val updatedAt: Long = System.currentTimeMillis()
)

data class RoutineItem(
    val id: Long = 0L,
    val title: String,
    val timeOfDay: RoutineTimeOfDay,
    val targetTime: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

data class RoutineLog(
    val id: Long = 0L,
    val routineId: Long,
    val dateKey: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

data class Medication(
    val id: Long = 0L,
    val name: String,
    val dosage: String,
    val frequency: String,
    val scheduledTimes: List<String>,
    val notes: String = "",
    val isActive: Boolean = true
)

data class MedicationLog(
    val id: Long = 0L,
    val medicationId: Long,
    val scheduledTime: String,
    val dateKey: String,
    val isTaken: Boolean = false,
    val takenAt: Long? = null
)

data class NutritionGuideline(
    val id: Long = 0L,
    val type: NutritionType,
    val description: String,
    val category: String,
    val activeDate: String,
    val doctorNotes: String = "",
    val isActive: Boolean = true
)

data class DailyJournalEntry(
    val id: Long = 0L,
    val entryDate: String,
    val moodScore: Int = 3,
    val struggles: String = "",
    val thankfulFor: String = "",
    val richTextContent: String = "",
    val heartRate: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class SavingsGoal(
    val id: Long = 0L,
    val goalName: String = "Emergency Medicine Fund",
    val targetAmount: Double = 500.0,
    val currentAmount: Double = 0.0,
    val currencySymbol: String = "₱",
    val targetDate: String? = null,
    val quickAdd1: Double = 20.0,
    val quickAdd2: Double = 50.0,
    val quickAdd3: Double = 100.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class SavingsTransaction(
    val id: Long = 0L,
    val goalId: Long = 1L,
    val goalName: String = "",
    val amount: Double,
    val type: String, // "ADD" or "SUBTRACT"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class QuoteOrVerse(
    val id: Int,
    val dayOfYear: Int,
    val quoteText: String,
    val referenceAuthor: String,
    val category: String, // "SCRIPTURE", "MOTIVATIONAL", "HEART_HEALTH"
    val isFavorite: Boolean = false
)
