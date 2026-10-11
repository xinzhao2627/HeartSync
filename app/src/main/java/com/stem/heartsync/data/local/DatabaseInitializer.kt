package com.stem.heartsync.data.local

import android.content.Context
import com.stem.heartsync.data.model.DoctorProfile
import com.stem.heartsync.data.model.Medication
import com.stem.heartsync.data.model.NutritionGuideline
import com.stem.heartsync.data.model.NutritionType
import com.stem.heartsync.data.model.PatientProfile
import com.stem.heartsync.data.model.QuoteOrVerse
import com.stem.heartsync.data.model.RoutineItem
import com.stem.heartsync.data.model.RoutineTimeOfDay
import com.stem.heartsync.data.model.SavingsGoal
import com.stem.heartsync.data.model.UserPreferences
import com.stem.heartsync.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseInitializer {

    fun initializeIfEmpty(context: Context) {
        val dbHelper = HeartSyncDbHelper.getInstance(context)

        // 1. Check Preferences
        val prefs = dbHelper.getUserPreferences()
        if (prefs.lastDoctorVisitTimestamp == null && prefs.updatedAt == 0L) {
            dbHelper.saveUserPreferences(
                UserPreferences(
                    id = 1L,
                    activeRole = UserRole.PATIENT,
                    classModeEnabled = false,
                    customRingtoneUri = null,
                    sfxEnabled = true,
                    lastDoctorVisitTimestamp = System.currentTimeMillis() - 86400000L, // 1 day ago
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 2. Doctor Profile
        val doctor = dbHelper.getDoctorProfile()
        if (doctor.doctorName.isEmpty() || doctor.updatedAt == 0L) {
            dbHelper.saveDoctorProfile(
                DoctorProfile(
                    id = 1L,
                    doctorName = "Dr. Maria Santos, MD",
                    specialty = "Cardiology & Vascular Medicine",
                    clinicName = "Philippine Heart Center Clinic",
                    contactPhone = "+63 917 555 0199",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 3. Patient Profile
        val patient = dbHelper.getPatientProfile()
        if (patient.patientName.isEmpty() || patient.updatedAt == 0L) {
            dbHelper.savePatientProfile(
                PatientProfile(
                    id = 1L,
                    patientName = "Juan Dela Cruz",
                    age = 19,
                    bloodType = "O+",
                    conditionNotes = "Stage 1 Essential Hypertension & Episodic Arrhythmia. Monitor resting pulse, avoid caffeinated energy drinks, stay well hydrated.",
                    emergencyContactName = "Rosa Dela Cruz (Guardian) / School Clinic",
                    emergencyContactPhone = "+63 918 555 0122",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 4. Default Routines (Morning, Afternoon, Night)
        val routines = dbHelper.getAllRoutines()
        if (routines.isEmpty()) {
            dbHelper.upsertRoutine(
                RoutineItem(
                    title = "Log baseline resting blood pressure and heart rate",
                    timeOfDay = RoutineTimeOfDay.MORNING,
                    targetTime = "07:00",
                    sortOrder = 1
                )
            )
            dbHelper.upsertRoutine(
                RoutineItem(
                    title = "Glass of warm water (300ml) with morning meal",
                    timeOfDay = RoutineTimeOfDay.MORNING,
                    targetTime = "07:30",
                    sortOrder = 2
                )
            )
            dbHelper.upsertRoutine(
                RoutineItem(
                    title = "Afternoon hydration check (500ml water bottle)",
                    timeOfDay = RoutineTimeOfDay.AFTERNOON,
                    targetTime = "13:00",
                    sortOrder = 3
                )
            )
            dbHelper.upsertRoutine(
                RoutineItem(
                    title = "5-minute slow rhythmic breathing exercise between classes",
                    timeOfDay = RoutineTimeOfDay.AFTERNOON,
                    targetTime = "15:30",
                    sortOrder = 4
                )
            )
            dbHelper.upsertRoutine(
                RoutineItem(
                    title = "Evening cardiac check-in & Daily Journal entry",
                    timeOfDay = RoutineTimeOfDay.NIGHT,
                    targetTime = "20:30",
                    sortOrder = 5
                )
            )
        }

        // 5. Default Medications (Time-based)
        val meds = dbHelper.getAllMedications()
        if (meds.isEmpty()) {
            dbHelper.upsertMedication(
                Medication(
                    name = "Bisoprolol 2.5mg",
                    dosage = "1 tablet with breakfast",
                    frequency = "Once daily",
                    scheduledTimes = listOf("08:00"),
                    notes = "Prescribed for resting heart rate stabilization. Do not crush."
                )
            )
            dbHelper.upsertMedication(
                Medication(
                    name = "Potassium Citrate 1080mg",
                    dosage = "1 tablet after lunch",
                    frequency = "Once daily",
                    scheduledTimes = listOf("13:00"),
                    notes = "Electrolyte balance support. Drink with a full glass of water."
                )
            )
            dbHelper.upsertMedication(
                Medication(
                    name = "Magnesium Glycinate 200mg",
                    dosage = "1 capsule before bed",
                    frequency = "Once daily",
                    scheduledTimes = listOf("21:00"),
                    notes = "Supports muscular vascular tone and restful sleep."
                )
            )
        }

        // 6. Default Nutrition Guidelines (Foods to Eat vs Foods to Avoid)
        val nutrition = dbHelper.getAllNutritionGuidelines()
        if (nutrition.isEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dbHelper.upsertNutritionGuideline(
                NutritionGuideline(
                    type = NutritionType.TO_EAT,
                    description = "High-potassium fruits (bananas, oranges, avocados) to shield heart muscle from arrhythmias.",
                    category = "Electrolyte Balance",
                    activeDate = todayStr,
                    doctorNotes = "Eat at least 1 banana or citrus fruit during school break."
                )
            )
            dbHelper.upsertNutritionGuideline(
                NutritionGuideline(
                    type = NutritionType.TO_EAT,
                    description = "Steamed leafy greens (kangkong, spinach) and oatmeal for nitric oxide vascular elasticity.",
                    category = "Vascular Health",
                    activeDate = todayStr,
                    doctorNotes = "Swap cafeteria fried snacks for oatmeal or boiled corn."
                )
            )
            dbHelper.upsertNutritionGuideline(
                NutritionGuideline(
                    type = NutritionType.TO_AVOID,
                    description = "Energy drinks, canned espresso, and pre-workout drinks (Caffeine overdose tachycardia risk).",
                    category = "Caffeine & Stimulants",
                    activeDate = todayStr,
                    doctorNotes = "Strictly zero energy drinks. Directly triggers cardiac palpitations."
                )
            )
            dbHelper.upsertNutritionGuideline(
                NutritionGuideline(
                    type = NutritionType.TO_AVOID,
                    description = "High-sodium instant cup noodles, canned luncheon meats, and salted chips (>400mg sodium).",
                    category = "Sodium Restriction",
                    activeDate = todayStr,
                    doctorNotes = "Sodium retains arterial fluid and increases blood pressure."
                )
            )
        }

        // 7. Default Savings Goal (₱500 default)
        if (!dbHelper.hasSavingsGoal()) {
            dbHelper.updateSavingsGoal(
                SavingsGoal(
                    id = 1L,
                    goalName = "Emergency Medicine Fund",
                    targetAmount = 500.0,
                    currentAmount = 250.0,
                    currencySymbol = "₱"
                )
            )
            dbHelper.applySavingsDelta(250.0, "Initial seed: medicine emergency fund")
        } else {
            val existing = dbHelper.getSavingsGoal()
            if (existing.goalName.contains("Buffer", ignoreCase = true)) {
                dbHelper.updateSavingsGoal(existing.copy(goalName = "Emergency Medicine Fund"))
            }
        }

        // 8. Offline Seed: 365 Daily Quotes & Bible Verses
        if (!dbHelper.hasQuotes()) {
            seedQuotesAndVerses(dbHelper)
        }
    }

    private fun seedQuotesAndVerses(dbHelper: HeartSyncDbHelper) {
        val curatedQuotes = listOf(
            QuoteOrVerse(
                id = 1,
                dayOfYear = 1,
                quoteText = "I can do all things through Christ who strengthens me.",
                referenceAuthor = "Philippians 4:13",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 2,
                dayOfYear = 2,
                quoteText = "Above all else, guard your heart, for everything you do flows from it.",
                referenceAuthor = "Proverbs 4:23",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 3,
                dayOfYear = 3,
                quoteText = "A cheerful heart is good medicine, but a crushed spirit dries up the bones.",
                referenceAuthor = "Proverbs 17:22",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 4,
                dayOfYear = 4,
                quoteText = "For I know the plans I have for you, plans to prosper you and not to harm you, plans to give you hope and a future.",
                referenceAuthor = "Jeremiah 29:11",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 5,
                dayOfYear = 5,
                quoteText = "Peace I leave with you; my peace I give you. Do not let your hearts be troubled and do not be afraid.",
                referenceAuthor = "John 14:27",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 6,
                dayOfYear = 6,
                quoteText = "The Lord is my strength and my shield; my heart trusts in him, and he helps me.",
                referenceAuthor = "Psalm 28:7",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 7,
                dayOfYear = 7,
                quoteText = "Those who hope in the Lord will renew their strength. They will soar on wings like eagles.",
                referenceAuthor = "Isaiah 40:31",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 8,
                dayOfYear = 8,
                quoteText = "Come to me, all you who are weary and burdened, and I will give you rest.",
                referenceAuthor = "Matthew 11:28",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 9,
                dayOfYear = 9,
                quoteText = "My flesh and my heart may fail, but God is the strength of my heart and my portion forever.",
                referenceAuthor = "Psalm 73:26",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 10,
                dayOfYear = 10,
                quoteText = "Be strong and courageous. Do not be afraid; do not be discouraged, for the Lord your God will be with you wherever you go.",
                referenceAuthor = "Joshua 1:9",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 11,
                dayOfYear = 11,
                quoteText = "Cast all your anxiety on him because he cares for you.",
                referenceAuthor = "1 Peter 5:7",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 12,
                dayOfYear = 12,
                quoteText = "Trust in the Lord with all your heart and lean not on your own understanding.",
                referenceAuthor = "Proverbs 3:5-6",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 13,
                dayOfYear = 13,
                quoteText = "The Lord is my shepherd; I lack nothing. He leads me beside quiet waters, he refreshes my soul.",
                referenceAuthor = "Psalm 23:1-3",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 14,
                dayOfYear = 14,
                quoteText = "For God gave us a spirit not of fear but of power and love and self-control.",
                referenceAuthor = "2 Timothy 1:7",
                category = "SCRIPTURE"
            ),
            QuoteOrVerse(
                id = 15,
                dayOfYear = 15,
                quoteText = "You can do this! Every breath is a victory and each day is a step forward.",
                referenceAuthor = "HeartSync Motivation",
                category = "MOTIVATIONAL"
            ),
            QuoteOrVerse(
                id = 16,
                dayOfYear = 16,
                quoteText = "Your heart beats with purpose. Rest when needed, but never doubt your courage.",
                referenceAuthor = "Cardiology Inspiration",
                category = "MOTIVATIONAL"
            ),
            QuoteOrVerse(
                id = 17,
                dayOfYear = 17,
                quoteText = "Small healthy habits today build the strongest foundation for tomorrow's dreams.",
                referenceAuthor = "Student Health Journal",
                category = "MOTIVATIONAL"
            )
        )

        // Generate full 365 deterministic entries
        val full365List = (1..365).map { day ->
            val existing = curatedQuotes.find { it.dayOfYear == day }
            if (existing != null) {
                existing
            } else {
                val cycleIdx = (day - 1) % curatedQuotes.size
                val template = curatedQuotes[cycleIdx]
                QuoteOrVerse(
                    id = day,
                    dayOfYear = day,
                    quoteText = template.quoteText,
                    referenceAuthor = template.referenceAuthor,
                    category = template.category
                )
            }
        }

        dbHelper.insertQuotesBatch(full365List)
    }
}
