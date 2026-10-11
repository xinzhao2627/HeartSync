package com.stem.heartsync.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.Medication
import com.stem.heartsync.data.model.NutritionGuideline
import com.stem.heartsync.data.model.NutritionType
import com.stem.heartsync.data.model.RoutineItem
import com.stem.heartsync.data.model.RoutineTimeOfDay
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.theme.*
import java.util.Calendar

@Composable
fun PatientRoutinesMedsScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    modifier: Modifier = Modifier
) {
    val routines by repository.routines.collectAsState()
    val routineLogs by repository.routineLogs.collectAsState()
    val medications by repository.medications.collectAsState()
    val medicationLogs by repository.medicationLogs.collectAsState()
    val nutrition by repository.nutritionGuidelines.collectAsState()
    val prefs by repository.userPreferences.collectAsState()

    // Determine current time of day for progressive disclosure
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val defaultExpandedTimeOfDay = when {
        currentHour in 5..11 -> RoutineTimeOfDay.MORNING
        currentHour in 12..17 -> RoutineTimeOfDay.AFTERNOON
        else -> RoutineTimeOfDay.NIGHT
    }

    var expandedSection by remember { mutableStateOf<RoutineTimeOfDay?>(defaultExpandedTimeOfDay) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. ROUTINES SECTION ---
        item {
            Text(
                text = "Prescribed Daily Routines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepAnchorNavy900
            )
        }

        // Morning Card (Rose Pulse Pink & Crimson)
        item {
            RoutineTimeCard(
                timeOfDay = RoutineTimeOfDay.MORNING,
                icon = Icons.Default.WbSunny,
                accentColor = CardiacCrimson600,
                containerColor = SoftBlushCard,
                isExpanded = expandedSection == RoutineTimeOfDay.MORNING,
                onToggleExpand = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    expandedSection = if (expandedSection == RoutineTimeOfDay.MORNING) null else RoutineTimeOfDay.MORNING
                },
                items = routines.filter { it.timeOfDay == RoutineTimeOfDay.MORNING },
                completedMap = routineLogs,
                onToggleItem = { routineId, isChecked ->
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    repository.toggleRoutineCompleted(routineId, isChecked)
                }
            )
        }

        // Afternoon Card (Soft Sky Blue & Electric Blue)
        item {
            RoutineTimeCard(
                timeOfDay = RoutineTimeOfDay.AFTERNOON,
                icon = Icons.Default.LightMode,
                accentColor = ElectricBlue,
                containerColor = SoftSkyCard,
                isExpanded = expandedSection == RoutineTimeOfDay.AFTERNOON,
                onToggleExpand = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    expandedSection = if (expandedSection == RoutineTimeOfDay.AFTERNOON) null else RoutineTimeOfDay.AFTERNOON
                },
                items = routines.filter { it.timeOfDay == RoutineTimeOfDay.AFTERNOON },
                completedMap = routineLogs,
                onToggleItem = { routineId, isChecked ->
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    repository.toggleRoutineCompleted(routineId, isChecked)
                }
            )
        }

        // Night Card (Soft Ice Blue & Deep Anchor Navy)
        item {
            RoutineTimeCard(
                timeOfDay = RoutineTimeOfDay.NIGHT,
                icon = Icons.Default.Bedtime,
                accentColor = DeepAnchorNavy800,
                containerColor = DeepAnchorNavy100,
                isExpanded = expandedSection == RoutineTimeOfDay.NIGHT,
                onToggleExpand = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    expandedSection = if (expandedSection == RoutineTimeOfDay.NIGHT) null else RoutineTimeOfDay.NIGHT
                },
                items = routines.filter { it.timeOfDay == RoutineTimeOfDay.NIGHT },
                completedMap = routineLogs,
                onToggleItem = { routineId, isChecked ->
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    repository.toggleRoutineCompleted(routineId, isChecked)
                }
            )
        }

        // --- 2. TIME-BASED MEDICATIONS TIMELINE ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medication Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Time-Based",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepAnchorNavy600
                )
            }
        }

        if (medications.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No medications currently prescribed by doctor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Flatten medications by scheduled times for timeline rendering
            val timelineItems = mutableListOf<Triple<Medication, String, Boolean>>()
            for (med in medications) {
                for (time in med.scheduledTimes) {
                    val key = "${med.id}_$time"
                    val isTaken = medicationLogs[key] == true
                    timelineItems.add(Triple(med, time, isTaken))
                }
            }
            timelineItems.sortBy { it.second }

            items(timelineItems) { (med, time, isTaken) ->
                MedicationTimelineCard(
                    medication = med,
                    scheduledTime = time,
                    isTaken = isTaken,
                    onToggleTaken = { taken ->
                        audioManager.playSfx(
                            if (taken) HeartSyncAudioManager.SFX_SUCCESS else HeartSyncAudioManager.SFX_CLICK,
                            prefs.classModeEnabled,
                            prefs.sfxEnabled
                        )
                        repository.toggleMedicationIntake(med.id, time, taken)
                    }
                )
            }
        }

        // --- 3. CLINICAL DIETARY DIRECTIVES ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clinical Dietary Directives",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
                Text(
                    text = "Doctor Recommended",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepAnchorNavy600
                )
            }
        }

        val toEat = nutrition.filter { it.type == NutritionType.TO_EAT }
        val toAvoid = nutrition.filter { it.type == NutritionType.TO_AVOID }

        // Foods to Eat Card (Soft Sky Blue)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftSkyCard,
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Heart-Healthy Fuel (To Eat)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                            Text(
                                text = "Nutrients supporting cardiac stability & endurance",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (toEat.isEmpty()) {
                        Text(
                            text = "No specific guidelines provided yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                    } else {
                        toEat.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepAnchorNavy900
                                    )
                                    if (item.category.isNotEmpty() || item.doctorNotes.isNotEmpty()) {
                                        Text(
                                            text = listOf(item.category, item.doctorNotes).filter { it.isNotEmpty() }.joinToString(" • "),
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

        // Foods to Avoid Card (Soft Blush Pink)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftBlushCard,
                border = BorderStroke(1.dp, CardiacCrimson600.copy(alpha = 0.2f)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardiacCrimson600.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CardiacCrimson600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Arrhythmia & BP Triggers (To Avoid)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CardiacCrimson900
                            )
                            Text(
                                text = "Foods known to spike palpitations and blood pressure",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (toAvoid.isEmpty()) {
                        Text(
                            text = "No restriction items assigned yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                    } else {
                        toAvoid.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✕",
                                    color = CardiacCrimson600,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepAnchorNavy900
                                    )
                                    if (item.category.isNotEmpty() || item.doctorNotes.isNotEmpty()) {
                                        Text(
                                            text = listOf(item.category, item.doctorNotes).filter { it.isNotEmpty() }.joinToString(" • "),
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
    }
}

@Composable
private fun RoutineTimeCard(
    timeOfDay: RoutineTimeOfDay,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    items: List<RoutineItem>,
    completedMap: Map<Long, Boolean>,
    onToggleItem: (Long, Boolean) -> Unit
) {
    val total = items.size
    val doneCount = items.count { completedMap[it.id] == true }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row (Clickable to collapse/expand without dark ripple)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }} Routine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$doneCount of $total completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepAnchorNavy600
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = DeepAnchorNavy600
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (items.isEmpty()) {
                        Text(
                            text = "No routines scheduled for this time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        items.forEach { item ->
                            val isChecked = completedMap[item.id] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onToggleItem(item.id, !isChecked) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleItem(item.id, it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ElectricBlue,
                                        uncheckedColor = DeepAnchorNavy600
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Target: ${item.targetTime}",
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
}

@Composable
private fun MedicationTimelineCard(
    medication: Medication,
    scheduledTime: String,
    isTaken: Boolean,
    onToggleTaken: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isTaken) SoftSkyCard else SurgicalWhite,
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
            // Time Badge & Pill Graphic
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTaken) ElectricBlue else CardiacCrimson600
                ) {
                    Text(
                        text = scheduledTime,
                        color = SurgicalWhite,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = medication.dosage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepAnchorNavy600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (medication.notes.isNotEmpty()) {
                        Text(
                            text = medication.notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button
            if (isTaken) {
                OutlinedButton(
                    onClick = { onToggleTaken(false) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                    border = BorderStroke(1.dp, ElectricBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Taken", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onToggleTaken(true) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Take", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
