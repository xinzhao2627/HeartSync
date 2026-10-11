package com.stem.heartsync.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.*
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorCockpitScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onSwitchBackToPatient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val doctor by repository.doctorProfile.collectAsState()
    val patient by repository.patientProfile.collectAsState()
    val routines by repository.routines.collectAsState()
    val routineLogs by repository.routineLogs.collectAsState()
    val medications by repository.medications.collectAsState()
    val medicationLogs by repository.medicationLogs.collectAsState()
    val nutrition by repository.nutritionGuidelines.collectAsState()
    val prefs by repository.userPreferences.collectAsState()

    val doctorPagerState = rememberPagerState(pageCount = { 4 })

    // Dialog states for Doctor CRUD operations
    var showAddMedDialog by remember { mutableStateOf(false) }
    var medName by remember { mutableStateOf("") }
    var medDosage by remember { mutableStateOf("1 tablet with breakfast") }
    var medTime by remember { mutableStateOf("08:00") }
    var medNotes by remember { mutableStateOf("Take after food") }
    var showMedClockPicker by remember { mutableStateOf(false) }

    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var routineTitle by remember { mutableStateOf("") }
    var routineTimeOfDay by remember { mutableStateOf(RoutineTimeOfDay.MORNING) }
    var routineTargetTime by remember { mutableStateOf("07:30") }
    var showRoutineClockPicker by remember { mutableStateOf(false) }

    fun formatDisplayTime(time24: String): String {
        val parts = time24.split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: return time24
            val m = parts[1].toIntOrNull() ?: return time24
            val amPm = if (h >= 12) "PM" else "AM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return String.format(Locale.getDefault(), "%02d:%02d %s", h12, m, amPm)
        }
        return time24
    }

    var showAddFoodDialog by remember { mutableStateOf(false) }
    var foodType by remember { mutableStateOf(NutritionType.TO_EAT) }
    var foodDesc by remember { mutableStateOf("") }
    var foodCategory by remember { mutableStateOf("Electrolytes") }

    var showEditConditionDialog by remember { mutableStateOf(false) }
    var conditionInput by remember { mutableStateOf(patient.conditionNotes) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground)
    ) {
        // 0. Greeting H2 Below Navbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
        ) {
            Text(
                text = "Hi, ${doctor.doctorName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DeepAnchorNavy900
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your assigned student is ${patient.patientName}.",
                style = MaterialTheme.typography.bodyMedium,
                color = DeepAnchorNavy600,
                lineHeight = 20.sp
            )
        }

        // 1. Doctor Care Station Banner
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DeepAnchorNavy800,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Doctor Care Station",
                            style = MaterialTheme.typography.titleLarge,
                            color = SurgicalWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${doctor.specialty} • ${doctor.clinicName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SurgicalWhite.copy(alpha = 0.85f)
                        )
                    }

                    IconButton(
                        onClick = onSwitchBackToPatient,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurgicalWhite.copy(alpha = 0.15f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Doctor Mode", tint = SurgicalWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurgicalWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            repository.recordDoctorVisitAndNotify()
                            Toast.makeText(
                                context,
                                "Notification sent: Doctor ${doctor.doctorName} visited your heart!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = CardiacCrimson600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notify Patient of Check-in",
                            color = CardiacCrimson900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Swipable Synchronized Tabs Row
        val tabs = listOf("💊 Medicines", "📋 Routines", "🥗 Food Guide", "🩺 Notes")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = doctorPagerState.currentPage == index
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CardiacCrimson600 else SurgicalWhite,
                    shadowElevation = if (isSelected) 2.dp else 1.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                            coroutineScope.launch {
                                doctorPagerState.animateScrollToPage(index)
                            }
                        }
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SurgicalWhite else DeepAnchorNavy800,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 3. Swipable Pages via HorizontalPager
        HorizontalPager(
            state = doctorPagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    // MEDICINES TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Prescribed Medicines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddMedDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prescribe")
                                }
                            }
                        }

                        if (medications.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurgicalWhite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No medications prescribed yet. Tap '+ Prescribe' to add.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(medications) { med ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurgicalWhite,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = med.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "${med.dosage} • ${med.scheduledTimes.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = DeepAnchorNavy600)
                                            if (med.notes.isNotBlank()) {
                                                Text(text = med.notes, style = MaterialTheme.typography.bodySmall, color = CardiacCrimson600)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                                repository.deleteMedication(med.id, isDoctorAction = true)
                                                Toast.makeText(context, "Medicine removed", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ROUTINES TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Daily Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddRoutineDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Routine")
                                }
                            }
                        }

                        if (routines.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurgicalWhite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No daily routines assigned yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(routines) { routine ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SoftMintCard,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = routine.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${routine.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }} • Target: ${routine.targetTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DeepAnchorNavy600
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                                repository.deleteRoutine(routine.id, isDoctorAction = true)
                                                Toast.makeText(context, "Routine removed", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // FOOD DIRECTIVES TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Clinical Food Guidelines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddFoodDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Food")
                                }
                            }
                        }

                        if (nutrition.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SurgicalWhite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No food guidelines configured yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        } else {
                            items(nutrition) { item ->
                                val isEat = item.type == NutritionType.TO_EAT
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isEat) SoftMintCard else SoftBlushCard,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEat) "✓ TO EAT" else "⚠ TO AVOID",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEat) VitalityMint else CardiacCrimson600
                                            )
                                            Text(text = item.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = item.category, style = MaterialTheme.typography.bodySmall, color = DeepAnchorNavy600)
                                        }
                                        IconButton(
                                            onClick = {
                                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                                repository.deleteNutritionGuideline(item.id, isDoctorAction = true)
                                                Toast.makeText(context, "Guideline removed", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // PATIENT NOTES & VITALS
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SurgicalWhite,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Clinical Notes & Diagnosis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { showEditConditionDialog = true }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Condition", tint = CardiacCrimson600)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = patient.conditionNotes, style = MaterialTheme.typography.bodyLarge, lineHeight = 22.sp)

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(text = "Emergency Contact: ${patient.emergencyContactName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Phone: ${patient.emergencyContactPhone}", style = MaterialTheme.typography.bodyMedium, color = DeepAnchorNavy600)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR DOCTOR CRUD ---

    // --- POLISHED DIALOGS FOR DOCTOR STATION ---

    // 1. Add Medication Dialog
    if (showAddMedDialog) {
        AlertDialog(
            onDismissRequest = { showAddMedDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardiacCrimson600.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = CardiacCrimson600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Prescribe Medication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DeepAnchorNavy100,
                        focusedContainerColor = SoftSkyCard.copy(alpha = 0.35f),
                        unfocusedContainerColor = SurgicalWhite
                    )
                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text("Medicine Name (e.g. Bisoprolol)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = medDosage,
                        onValueChange = { medDosage = it },
                        label = { Text("Dosage (e.g. 2.5mg, 1 tab)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Interactive Time Selection Clock Field
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSkyCard.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, false, true)
                                showMedClockPicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scheduled Intake Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                                Text(
                                    text = "${formatDisplayTime(medTime)}  ($medTime)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = medNotes,
                        onValueChange = { medNotes = it },
                        label = { Text("Instructions (e.g. with breakfast)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (medName.isNotBlank()) {
                            repository.saveMedication(
                                Medication(
                                    name = medName,
                                    dosage = medDosage,
                                    frequency = "Daily",
                                    scheduledTimes = listOf(medTime),
                                    notes = medNotes
                                ),
                                isDoctorAction = true
                            )
                            showAddMedDialog = false
                            medName = ""
                            Toast.makeText(context, "Medication prescribed & notification dispatched!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Prescribe", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddMedDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // 2. Add Routine Dialog
    if (showAddRoutineDialog) {
        AlertDialog(
            onDismissRequest = { showAddRoutineDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Configure Routine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DeepAnchorNavy100,
                        focusedContainerColor = SoftSkyCard.copy(alpha = 0.35f),
                        unfocusedContainerColor = SurgicalWhite
                    )
                    OutlinedTextField(
                        value = routineTitle,
                        onValueChange = { routineTitle = it },
                        label = { Text("Routine Title (e.g. 10m Gentle Walk)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = routineTimeOfDay == RoutineTimeOfDay.MORNING,
                            onClick = { routineTimeOfDay = RoutineTimeOfDay.MORNING },
                            label = { Text("Morning") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftBlushCard,
                                selectedLabelColor = CardiacCrimson900
                            )
                        )
                        FilterChip(
                            selected = routineTimeOfDay == RoutineTimeOfDay.AFTERNOON,
                            onClick = { routineTimeOfDay = RoutineTimeOfDay.AFTERNOON },
                            label = { Text("Afternoon") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftSkyCard,
                                selectedLabelColor = ElectricBlue
                            )
                        )
                        FilterChip(
                            selected = routineTimeOfDay == RoutineTimeOfDay.NIGHT,
                            onClick = { routineTimeOfDay = RoutineTimeOfDay.NIGHT },
                            label = { Text("Night") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepAnchorNavy100,
                                selectedLabelColor = DeepAnchorNavy900
                            )
                        )
                    }
                    // Interactive Time Selection Clock Field
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSkyCard.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, false, true)
                                showRoutineClockPicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target Routine Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                                Text(
                                    text = "${formatDisplayTime(routineTargetTime)}  ($routineTargetTime)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routineTitle.isNotBlank()) {
                            repository.saveRoutine(
                                RoutineItem(
                                    title = routineTitle,
                                    timeOfDay = routineTimeOfDay,
                                    targetTime = routineTargetTime
                                ),
                                isDoctorAction = true
                            )
                            showAddRoutineDialog = false
                            routineTitle = ""
                            Toast.makeText(context, "Routine saved & notification dispatched!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Routine", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddRoutineDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // 3. Add Food Dialog
    if (showAddFoodDialog) {
        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Dietary Directive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DeepAnchorNavy100,
                        focusedContainerColor = SoftSkyCard.copy(alpha = 0.35f),
                        unfocusedContainerColor = SurgicalWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = foodType == NutritionType.TO_EAT,
                            onClick = { foodType = NutritionType.TO_EAT },
                            label = { Text("Foods to Eat") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftSkyCard,
                                selectedLabelColor = ElectricBlue
                            )
                        )
                        FilterChip(
                            selected = foodType == NutritionType.TO_AVOID,
                            onClick = { foodType = NutritionType.TO_AVOID },
                            label = { Text("Foods to Avoid") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftBlushCard,
                                selectedLabelColor = CardiacCrimson900
                            )
                        )
                    }
                    OutlinedTextField(
                        value = foodDesc,
                        onValueChange = { foodDesc = it },
                        label = { Text("Description (e.g. Potassium bananas)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = foodCategory,
                        onValueChange = { foodCategory = it },
                        label = { Text("Category (e.g. Sodium restriction)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (foodDesc.isNotBlank()) {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            repository.saveNutritionGuideline(
                                NutritionGuideline(
                                    type = foodType,
                                    description = foodDesc,
                                    category = foodCategory,
                                    activeDate = todayStr
                                ),
                                isDoctorAction = true
                            )
                            showAddFoodDialog = false
                            foodDesc = ""
                            Toast.makeText(context, "Dietary directive added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddFoodDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // 4. Edit Condition Dialog
    if (showEditConditionDialog) {
        AlertDialog(
            onDismissRequest = { showEditConditionDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardiacCrimson600.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CardiacCrimson600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Medical Condition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                OutlinedTextField(
                    value = conditionInput,
                    onValueChange = { conditionInput = it },
                    label = { Text("Diagnosis & Cardiology Instructions") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DeepAnchorNavy100,
                        focusedContainerColor = SoftSkyCard.copy(alpha = 0.35f),
                        unfocusedContainerColor = SurgicalWhite
                    ),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedPatient = patient.copy(conditionNotes = conditionInput)
                        repository.updatePatientProfile(updatedPatient, isDoctorAction = true)
                        showEditConditionDialog = false
                        Toast.makeText(context, "Condition updated & notification dispatched!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditConditionDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // --- TIME SELECTION CLOCK DIALOGS ---
    if (showMedClockPicker) {
        HeartSyncClockDialog(
            initialTime = medTime,
            onConfirm = { pickedTime ->
                medTime = pickedTime
                showMedClockPicker = false
            },
            onDismiss = { showMedClockPicker = false }
        )
    }

    if (showRoutineClockPicker) {
        HeartSyncClockDialog(
            initialTime = routineTargetTime,
            onConfirm = { pickedTime ->
                routineTargetTime = pickedTime
                showRoutineClockPicker = false
            },
            onDismiss = { showRoutineClockPicker = false }
        )
    }
}

// ---------------------------------------------------------------------------------
// Material 3 Analog/Digital Clock Dial Time Picker Dialog
// ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeartSyncClockDialog(
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = initialTime.split(":")
    val initH = initialParts.getOrNull(0)?.toIntOrNull() ?: 8
    val initM = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initH,
        initialMinute = initM,
        is24Hour = false
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurgicalWhite,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pick Schedule Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = SoftSkyCard,
                        clockDialSelectedContentColor = SurgicalWhite,
                        clockDialUnselectedContentColor = DeepAnchorNavy900,
                        selectorColor = CardiacCrimson600,
                        periodSelectorBorderColor = DeepAnchorNavy100,
                        periodSelectorSelectedContainerColor = CardiacCrimson600,
                        periodSelectorUnselectedContainerColor = SurgicalWhite,
                        periodSelectorSelectedContentColor = SurgicalWhite,
                        periodSelectorUnselectedContentColor = DeepAnchorNavy800,
                        timeSelectorSelectedContainerColor = SoftBlushCard,
                        timeSelectorUnselectedContainerColor = SoftSkyCard,
                        timeSelectorSelectedContentColor = CardiacCrimson900,
                        timeSelectorUnselectedContentColor = DeepAnchorNavy900
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formatted = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                            onConfirm(formatted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Time", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
