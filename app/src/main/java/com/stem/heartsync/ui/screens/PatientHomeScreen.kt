package com.stem.heartsync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.*
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.components.WeatherCard
import com.stem.heartsync.ui.theme.*
import com.stem.heartsync.weather.WeatherManager
import java.text.DecimalFormat

@Composable
fun PatientHomeScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onNavigateToRoutines: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToSavings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val patient by repository.patientProfile.collectAsState()
    val doctor by repository.doctorProfile.collectAsState()
    val prefs by repository.userPreferences.collectAsState()
    val routines by repository.routines.collectAsState()
    val routineLogs by repository.routineLogs.collectAsState()
    val medications by repository.medications.collectAsState()
    val medicationLogs by repository.medicationLogs.collectAsState()
    val quote by repository.todaysQuote.collectAsState()
    val savingsGoals by repository.savingsGoals.collectAsState()
    val currencyFormatter = remember { DecimalFormat("#,##0.00") }

    val totalRoutines = routines.size
    val doneRoutines = routines.count { routineLogs[it.id] == true }

    val todayKey by repository.currentDateKey.collectAsState()
    val journalEntries by repository.journalEntries.collectAsState()
    val todayJournal = journalEntries.find { it.entryDate == todayKey }
    val currentMood = todayJournal?.moodScore ?: 3
    val weatherManager = remember { WeatherManager.getInstance(context) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1 & 2. Hi ... and Your doctor is:
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Hi, ${patient.patientName}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepAnchorNavy900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your doctor is: ${doctor.doctorName}, ${doctor.specialty}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepAnchorNavy600,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Weather Today Card
        item {
            WeatherCard(
                weatherManager = weatherManager,
                audioManager = audioManager,
                classModeEnabled = prefs.classModeEnabled,
                sfxEnabled = prefs.sfxEnabled
            )
        }

        // 4. Medium Sized Quote Card (no lockscreen button, taller like a true card)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftBlushCard,
                border = BorderStroke(1.dp, RosePulse300.copy(alpha = 0.4f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 160.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RosePulse300.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = CardiacCrimson600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Daily Word for Your Heart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CardiacCrimson900
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "\"${quote?.quoteText ?: "I can do all things through Christ who strengthens me."}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = DeepAnchorNavy900,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "— ${quote?.referenceAuthor ?: "Philippians 4:13"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepAnchorNavy600,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // 5. Routines (Huge check if all done, have a cancel below to edit some toggle if user clicked accidentally)
        item {
            var showEditRoutinesWhenDone by remember { mutableStateOf(false) }
            val isAllDone = totalRoutines > 0 && doneRoutines == totalRoutines

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isAllDone && !showEditRoutinesWhenDone) SoftSkyCard else SoftMintCard,
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAllDone && !showEditRoutinesWhenDone) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "All Routines Completed",
                                tint = ElectricBlue,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All Routines Completed!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepAnchorNavy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You've completed all $totalRoutines prescribed routines for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                showEditRoutinesWhenDone = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ElectricBlue),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel / Edit Routines", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Prescribed Routines",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "$doneRoutines of $totalRoutines completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                            if (showEditRoutinesWhenDone) {
                                TextButton(
                                    onClick = {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                        showEditRoutinesWhenDone = false
                                    }
                                ) {
                                    Text("Done Editing", color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                TextButton(onClick = onNavigateToRoutines) {
                                    Text("View All", color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (routines.isEmpty()) {
                            Text(
                                text = "No routines scheduled yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepAnchorNavy600
                            )
                        } else {
                            routines.forEach { routine ->
                                val isDone = routineLogs[routine.id] == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                            repository.toggleRoutineCompleted(routine.id, !isDone)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { checked ->
                                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                            repository.toggleRoutineCompleted(routine.id, checked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ElectricBlue,
                                            uncheckedColor = DeepAnchorNavy600
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = routine.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                                            textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                            color = if (isDone) DeepAnchorNavy600 else DeepAnchorNavy900
                                        )
                                        Text(
                                            text = "${routine.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }} • ${routine.targetTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepAnchorNavy600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Heart Rhythm Pulse Dock
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurgicalWhite,
                border = BorderStroke(1.dp, DeepAnchorNavy100),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RosePulse100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Heart Rhythm Pulse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                        }
                        TextButton(onClick = onNavigateToJournal) {
                            Text("Diary ✎", color = CardiacCrimson600, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How does your heart feel right now?",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy600
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val moods = listOf(
                        1 to ("🌧️" to "Heavy"),
                        2 to ("⚡" to "Flutter"),
                        3 to ("💖" to "Calm"),
                        4 to ("🌿" to "Steady"),
                        5 to ("✨" to "Great")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        moods.forEach { (score, data) ->
                            val (emoji, label) = data
                            val isSelected = currentMood == score
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) RosePulse100 else Color.Transparent)
                                    .clickable {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                        val updated = (todayJournal ?: DailyJournalEntry(entryDate = todayKey)).copy(moodScore = score)
                                        repository.saveJournal(updated)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(text = emoji, fontSize = 26.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CardiacCrimson900 else DeepAnchorNavy600,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftBlushCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val message = when (currentMood) {
                            1 -> "Heavy rhythm noted. Please sit down, relax, and hydrate."
                            2 -> "Fluttery sensations noted. Practice slow, steady breathing."
                            3 -> "Calm and peaceful rhythm recorded for today."
                            4 -> "Smooth, steady heart rhythm. Keep up your gentle routine."
                            5 -> "Great and energetic! Pace yourself and enjoy your day."
                            else -> "Tap a sensation above to record your heart pulse."
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepAnchorNavy800,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // 7. Meds to Take (The medication timeline)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurgicalWhite,
                border = BorderStroke(1.dp, DeepAnchorNavy100),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CardiacCrimson600.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Meds to Take",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = "Today's scheduled medication timeline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        TextButton(onClick = onNavigateToRoutines) {
                            Text("Full Schedule", color = CardiacCrimson600, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val timelineDoses = remember(medications, medicationLogs) {
                        val list = mutableListOf<Triple<Medication, String, Boolean>>()
                        for (med in medications) {
                            for (time in med.scheduledTimes) {
                                val key = "${med.id}_$time"
                                val isTaken = medicationLogs[key] == true
                                list.add(Triple(med, time, isTaken))
                            }
                        }
                        list.sortedBy { it.second }
                    }

                    if (timelineDoses.isEmpty()) {
                        Text(
                            text = "No medications scheduled for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                    } else {
                        timelineDoses.forEach { (med, time, isTaken) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isTaken) SoftSkyCard else SoftBlushCard)
                                    .clickable {
                                        audioManager.playSfx(
                                            if (!isTaken) HeartSyncAudioManager.SFX_SUCCESS else HeartSyncAudioManager.SFX_CLICK,
                                            prefs.classModeEnabled,
                                            prefs.sfxEnabled
                                        )
                                        repository.toggleMedicationIntake(med.id, time, !isTaken)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isTaken) ElectricBlue else CardiacCrimson600
                                    ) {
                                        Text(
                                            text = time,
                                            color = SurgicalWhite,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = med.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepAnchorNavy900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = med.dosage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeepAnchorNavy600,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (isTaken) "Taken" else "Pending",
                                        tint = if (isTaken) ElectricBlue else DeepAnchorNavy600,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTaken) "Taken" else "Take",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTaken) ElectricBlue else DeepAnchorNavy600,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Savings Widget (Oldest item missing money or incomplete, dark navy & pink card design)
        item {
            val incompleteGoals = savingsGoals.filter { it.currentAmount < it.targetAmount }
            val oldestIncompleteGoal = incompleteGoals.minByOrNull { if (it.createdAt > 0) it.createdAt else it.id }
                ?: savingsGoals.firstOrNull()

            if (oldestIncompleteGoal != null) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = DeepAnchorNavy800,
                    border = BorderStroke(1.dp, DeepAnchorNavy600.copy(alpha = 0.4f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                            onNavigateToSavings()
                        }
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = oldestIncompleteGoal.goalName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RosePulse300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val remaining = (oldestIncompleteGoal.targetAmount - oldestIncompleteGoal.currentAmount).coerceAtLeast(0.0)
                                Text(
                                    text = if (remaining > 0) "₱${currencyFormatter.format(remaining)} still needed" else "Goal Complete! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurgicalWhite.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onNavigateToSavings,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, RosePulse300),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Open ₱",
                                        color = RosePulse300,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Piggybank icon on the RIGHT
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(DeepAnchorNavy900)
                                        .border(1.dp, RosePulse300.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = RosePulse300,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "₱${currencyFormatter.format(oldestIncompleteGoal.currentAmount)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SurgicalWhite,
                                maxLines = 1,
                                softWrap = false
                            )

                            val pct = if (oldestIncompleteGoal.targetAmount > 0) {
                                ((oldestIncompleteGoal.currentAmount / oldestIncompleteGoal.targetAmount) * 100).toInt()
                            } else 0

                            Text(
                                text = "$pct% Saved",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RosePulse300,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progressRatio = if (oldestIncompleteGoal.targetAmount > 0) {
                            (oldestIncompleteGoal.currentAmount / oldestIncompleteGoal.targetAmount).toFloat().coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = RosePulse300,
                            trackColor = DeepAnchorNavy900
                        )
                    }
                }
            }
        }
    }
}
