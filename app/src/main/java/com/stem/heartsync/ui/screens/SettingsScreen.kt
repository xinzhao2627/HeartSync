package com.stem.heartsync.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.LocationStatus
import com.stem.heartsync.data.model.UserRole
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.notification.HeartSyncNotificationManager
import com.stem.heartsync.ui.theme.*
import com.stem.heartsync.weather.WeatherManager

@Composable
fun SettingsScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onNavigateToDoctorCockpit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs by repository.userPreferences.collectAsState()
    val patient by repository.patientProfile.collectAsState()
    val doctor by repository.doctorProfile.collectAsState()

    var showEditStudentDialog by remember { mutableStateOf(false) }
    var editStudentName by remember { mutableStateOf("") }
    var editStudentAge by remember { mutableStateOf("") }
    var editStudentBlood by remember { mutableStateOf("") }
    var editStudentCondition by remember { mutableStateOf("") }
    var editStudentEmergencyName by remember { mutableStateOf("") }
    var editStudentEmergencyPhone by remember { mutableStateOf("") }

    var showEditDoctorDialog by remember { mutableStateOf(false) }
    var editDoctorName by remember { mutableStateOf("") }
    var editDoctorSpecialty by remember { mutableStateOf("") }
    var editDoctorClinic by remember { mutableStateOf("") }
    var editDoctorPhone by remember { mutableStateOf("") }

    val weatherManager = remember { WeatherManager.getInstance(context) }
    val weather by weatherManager.weather.collectAsState()
    val locationStatus by weatherManager.locationStatus.collectAsState()
    val selectedCity by weatherManager.selectedCity.collectAsState()
    var showWeatherCityDialog by remember { mutableStateOf(false) }
    var showWeatherTestDialog by remember { mutableStateOf(false) }

    // SAF Document Picker for audio/video file ringtones
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission handled
            }
            repository.setCustomRingtoneUri(uri.toString())
            Toast.makeText(context, "Custom ringtone saved!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Personal Information & Profiles",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepAnchorNavy900
            )
        }

        // 0. Student Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardiacCrimson600.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Student Profile",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepAnchorNavy600
                                )
                                Text(
                                    text = patient.patientName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                            }
                        }

                        Button(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                editStudentName = patient.patientName
                                editStudentAge = patient.age.toString()
                                editStudentBlood = patient.bloodType
                                editStudentCondition = patient.conditionNotes
                                editStudentEmergencyName = patient.emergencyContactName
                                editStudentEmergencyPhone = patient.emergencyContactPhone
                                showEditStudentDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Info", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Age ${patient.age} • Blood Type ${patient.bloodType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy800,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Condition: ${patient.conditionNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Emergency: ${patient.emergencyContactName} • ${patient.emergencyContactPhone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepAnchorNavy600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // 1. Healthcare Provider Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SoftSkyCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Assigned Healthcare Provider",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepAnchorNavy600
                                )
                                Text(
                                    text = doctor.doctorName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                            }
                        }

                        Button(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                editDoctorName = doctor.doctorName
                                editDoctorSpecialty = doctor.specialty
                                editDoctorClinic = doctor.clinicName
                                editDoctorPhone = doctor.contactPhone
                                showEditDoctorDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Doctor", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${doctor.specialty} • ${doctor.clinicName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy800,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Emergency Phone: ${doctor.contactPhone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepAnchorNavy600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Re-run Onboarding Setup
        item {
            OutlinedButton(
                onClick = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    repository.resetOnboarding()
                    Toast.makeText(context, "Setup restarted. Please fill your profile.", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-run Initial Setup / Onboarding", fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text(
                text = "Preferences & Sound",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepAnchorNavy900
            )
        }

        // 1. Class Mode Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurgicalWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (prefs.classModeEnabled) CardiacCrimson600 else DeepAnchorNavy100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (prefs.classModeEnabled) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                contentDescription = "Class Mode Status",
                                tint = if (prefs.classModeEnabled) SurgicalWhite else DeepAnchorNavy800,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Class Mode (Silent Vibrate)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                            Text(
                                text = if (prefs.classModeEnabled) "Active: Alarms vibrate only (no sound in class)" else "Audible alarms and sound enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Switch(
                        checked = prefs.classModeEnabled,
                        onCheckedChange = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, false, prefs.sfxEnabled)
                            repository.toggleClassMode()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurgicalWhite,
                            checkedTrackColor = CardiacCrimson600
                        )
                    )
                }
            }
        }

        // 2. Alarm Notifications (Medication & Routine Alarms)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurgicalWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardiacCrimson600.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CardiacCrimson600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Alarm Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                            Text(
                                text = "Choose which scheduled alerts can ring or vibrate",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DeepAnchorNavy100)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Medication Alarms Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RosePulse50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Medication Alarms",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = if (prefs.medicationAlarmsEnabled) "Alarms fire at scheduled medication intake times" else "Medication alarms turned off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        Switch(
                            checked = prefs.medicationAlarmsEnabled,
                            onCheckedChange = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                repository.toggleMedicationAlarms(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurgicalWhite,
                                checkedTrackColor = CardiacCrimson600
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DeepAnchorNavy100)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Routine Alarms Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftSkyCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Routine Alarms",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = if (prefs.routineAlarmsEnabled) "Alarms fire for morning, afternoon & night routines" else "Routine alarms turned off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        Switch(
                            checked = prefs.routineAlarmsEnabled,
                            onCheckedChange = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                repository.toggleRoutineAlarms(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurgicalWhite,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DeepAnchorNavy100)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Extreme Weather Alarms Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RosePulse100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Extreme Weather Alerts",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = if (prefs.weatherAlarmsEnabled) "Alarms fire for dangerous heat (≥35°C) or cold (≤19°C)" else "Weather alarms turned off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        Switch(
                            checked = prefs.weatherAlarmsEnabled,
                            onCheckedChange = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                repository.toggleWeatherAlarms(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SurgicalWhite,
                                checkedTrackColor = CardiacCrimson600
                            )
                        )
                    }

                    // Test Weather Alert button
                    if (prefs.weatherAlarmsEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                    showWeatherTestDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = DeepAnchorNavy900
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Test Weather Alert 🔔",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Custom Ringtone Picker (SAF)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftSkyCard,
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardiacCrimson600.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Custom Ringtone (SAF)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = if (prefs.customRingtoneUri != null) "Selected custom audio file" else "Default alarm audio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                audioPickerLauncher.launch(arrayOf("audio/*", "video/*"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Audio File")
                        }

                        if (prefs.customRingtoneUri != null) {
                            OutlinedButton(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                    repository.setCustomRingtoneUri(null)
                                    Toast.makeText(context, "Reset to default ringtone", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }

        // 3. Tactile Sound Effects (SoundPool)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SurgicalWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepAnchorNavy100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = DeepAnchorNavy800,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tactile SFX Feedback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                            Text(
                                text = if (prefs.sfxEnabled) "SoundPool clicks & completions active" else "Silent interface taps",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Switch(
                        checked = prefs.sfxEnabled,
                        onCheckedChange = { repository.toggleSfx(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurgicalWhite,
                            checkedTrackColor = DeepAnchorNavy800
                        )
                    )
                }
            }
        }

        // 4. Daily Lock Screen Quote (Midnight Dispatcher)
        item {
            val todaysQuote by repository.todaysQuote.collectAsState()
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftSkyCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Lock Screen Quote",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = "Scheduled daily at 12:00 AM midnight",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            val nm = HeartSyncNotificationManager.getInstance(context)
                            val qText = todaysQuote?.quoteText ?: "I can do all things through Christ who strengthens me."
                            val qAuthor = todaysQuote?.referenceAuthor ?: "Philippians 4:13"
                            nm.showDailyQuoteNotification(qText, qAuthor, prefs)
                            Toast.makeText(context, "Quote posted to lock screen! ✨", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Today's Quote to Lock Screen Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Weather & Location Telemetry Settings
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftSkyCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Weather & Location Telemetry",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = if (locationStatus == LocationStatus.AVAILABLE) {
                                        "GPS Active • Live Open-Meteo API"
                                    } else if (locationStatus == LocationStatus.GPS_OFF) {
                                        "Device Location is Off • Using preset"
                                    } else {
                                        "Location Permission Denied"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (locationStatus == LocationStatus.AVAILABLE) ElectricBlue else CardiacCrimson600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Active City: ${weather?.cityName ?: (selectedCity?.name ?: "Manila, PH")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepAnchorNavy800
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Location Settings")
                        }

                        OutlinedButton(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                showWeatherCityDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change City")
                        }
                    }

                    if (selectedCity != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                weatherManager.clearManualCity()
                                Toast.makeText(context, "Reset to GPS location detection", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset to GPS Location", color = ElectricBlue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 6. Healthcare Provider Management Switch
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftBlushCard,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardiacCrimson600.copy(alpha = 0.15f)),
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
                        Column {
                            Text(
                                text = "Doctor Mode (Shared Phone)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CardiacCrimson900
                            )
                            Text(
                                text = "Allows doctor to configure medicines and clinical routines",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepAnchorNavy600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            repository.switchRole(UserRole.DOCTOR)
                            onNavigateToDoctorCockpit()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600)
                    ) {
                        Text("Open Doctor Station", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 1. Edit Student Dialog
    if (showEditStudentDialog) {
        AlertDialog(
            onDismissRequest = { showEditStudentDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardiacCrimson600.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CardiacCrimson600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Student Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editStudentName,
                        onValueChange = { editStudentName = it },
                        label = { Text("Student Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editStudentAge,
                            onValueChange = { if (it.all { c -> c.isDigit() }) editStudentAge = it },
                            label = { Text("Age") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editStudentBlood,
                            onValueChange = { editStudentBlood = it },
                            label = { Text("Blood Type") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editStudentCondition,
                        onValueChange = { editStudentCondition = it },
                        label = { Text("Heart Condition & Notes") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editStudentEmergencyName,
                        onValueChange = { editStudentEmergencyName = it },
                        label = { Text("Emergency Contact (Name)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editStudentEmergencyPhone,
                        onValueChange = { editStudentEmergencyPhone = it },
                        label = { Text("Emergency Contact (Phone)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editStudentName.trim()
                        if (trimmed.isNotEmpty()) {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            repository.updatePatientProfile(
                                patient.copy(
                                    patientName = trimmed,
                                    age = editStudentAge.toIntOrNull() ?: patient.age,
                                    bloodType = editStudentBlood.trim().ifBlank { patient.bloodType },
                                    conditionNotes = editStudentCondition.trim().ifBlank { patient.conditionNotes },
                                    emergencyContactName = editStudentEmergencyName.trim().ifBlank { patient.emergencyContactName },
                                    emergencyContactPhone = editStudentEmergencyPhone.trim().ifBlank { patient.emergencyContactPhone }
                                )
                            )
                            showEditStudentDialog = false
                            Toast.makeText(context, "Student profile updated! ✨", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditStudentDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // 2. Edit Doctor Dialog
    if (showEditDoctorDialog) {
        AlertDialog(
            onDismissRequest = { showEditDoctorDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurgicalWhite,
            tonalElevation = 6.dp,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftSkyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Edit Healthcare Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editDoctorName,
                        onValueChange = { editDoctorName = it },
                        label = { Text("Doctor Full Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDoctorSpecialty,
                        onValueChange = { editDoctorSpecialty = it },
                        label = { Text("Specialty") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDoctorClinic,
                        onValueChange = { editDoctorClinic = it },
                        label = { Text("Clinic / Hospital Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDoctorPhone,
                        onValueChange = { editDoctorPhone = it },
                        label = { Text("Emergency Contact Phone") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editDoctorName.trim()
                        if (trimmed.isNotEmpty()) {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            repository.updateDoctorProfile(
                                doctor.copy(
                                    doctorName = trimmed,
                                    specialty = editDoctorSpecialty.trim().ifBlank { doctor.specialty },
                                    clinicName = editDoctorClinic.trim().ifBlank { doctor.clinicName },
                                    contactPhone = editDoctorPhone.trim().ifBlank { doctor.contactPhone }
                                )
                            )
                            showEditDoctorDialog = false
                            Toast.makeText(context, "Doctor profile updated! ✨", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDoctorDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DeepAnchorNavy600)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showWeatherCityDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherCityDialog = false },
            containerColor = SurgicalWhite,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Select Campus Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your city to fetch weather without enabling GPS location services:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy600
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(weatherManager.cityPresets) { city ->
                            val isSelected = selectedCity?.name == city.name
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SoftBlushCard else PaperBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CardiacCrimson600 else DeepAnchorNavy100
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                                        weatherManager.selectPresetCity(city)
                                        showWeatherCityDialog = false
                                        Toast.makeText(context, "Location set to ${city.name} 📍", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CardiacCrimson900 else DeepAnchorNavy900
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CardiacCrimson600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWeatherCityDialog = false }
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = DeepAnchorNavy900)
                }
            }
        )
    }

    if (showWeatherTestDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherTestDialog = false },
            containerColor = SurgicalWhite,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Test Weather Alarm Notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Trigger a live test alert with sound and vibration to test how HeartSync alerts students during extreme heat or cold conditions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepAnchorNavy600
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            weatherManager.triggerTestWeatherAlert(isHighHeat = true)
                            showWeatherTestDialog = false
                            Toast.makeText(context, "High Heat Warning alert sent! Check notification drawer 🔔", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔥 Test High Heat Alert (≥35°C)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                            weatherManager.triggerTestWeatherAlert(isHighHeat = false)
                            showWeatherTestDialog = false
                            Toast.makeText(context, "Cold Weather alert sent! Check notification drawer 🔔", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("❄️ Test Cold Weather Alert (≤19°C)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showWeatherTestDialog = false }
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = DeepAnchorNavy900)
                }
            }
        )
    }
}
