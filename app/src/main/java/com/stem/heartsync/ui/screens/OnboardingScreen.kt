package com.stem.heartsync.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.DoctorProfile
import com.stem.heartsync.data.model.PatientProfile
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.theme.*

@Composable
fun OnboardingScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPatient by repository.patientProfile.collectAsState()
    val currentDoctor by repository.doctorProfile.collectAsState()
    val prefs by repository.userPreferences.collectAsState()

    var step by remember { mutableIntStateOf(1) } // 1: Student, 2: Doctor

    // Student fields
    var studentName by remember { mutableStateOf(currentPatient.patientName) }
    var ageText by remember { mutableStateOf(currentPatient.age.toString()) }
    var bloodType by remember { mutableStateOf(currentPatient.bloodType) }
    var conditionNotes by remember { mutableStateOf(currentPatient.conditionNotes) }
    var emergencyContactName by remember { mutableStateOf(currentPatient.emergencyContactName) }
    var emergencyContactPhone by remember { mutableStateOf(currentPatient.emergencyContactPhone) }

    // Doctor fields
    var doctorName by remember { mutableStateOf(currentDoctor.doctorName) }
    var specialty by remember { mutableStateOf(currentDoctor.specialty) }
    var clinicName by remember { mutableStateOf(currentDoctor.clinicName) }
    var contactPhone by remember { mutableStateOf(currentDoctor.contactPhone) }

    Scaffold(
        containerColor = PaperBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (step == 1) CardiacCrimson600 else RosePulse100
                ) {
                    Text(
                        text = "1. Student Profile",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (step == 1) SurgicalWhite else CardiacCrimson600,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (step == 2) ElectricBlue else SoftSkyCard
                ) {
                    Text(
                        text = "2. Healthcare Provider",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (step == 2) SurgicalWhite else ElectricBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { currentStep ->
                if (currentStep == 1) {
                    // STEP 1: Student Account Setup
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(RosePulse100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CardiacCrimson600,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Create Student Profile",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = "No login required. Enter your details below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.dp, DeepAnchorNavy100),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = studentName,
                                    onValueChange = { studentName = it },
                                    label = { Text("Student Full Name *") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ageText,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) ageText = it },
                                        label = { Text("Age") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = bloodType,
                                        onValueChange = { bloodType = it },
                                        label = { Text("Blood Type") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = conditionNotes,
                                    onValueChange = { conditionNotes = it },
                                    label = { Text("Heart Condition & Diagnosis Notes") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = emergencyContactName,
                                    onValueChange = { emergencyContactName = it },
                                    label = { Text("Emergency Contact (Guardian / Clinic)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = emergencyContactPhone,
                                    onValueChange = { emergencyContactPhone = it },
                                    label = { Text("Emergency Contact Phone") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (studentName.trim().isBlank()) {
                                    Toast.makeText(context, "Please enter your student name", Toast.LENGTH_SHORT).show()
                                } else {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                                    step = 2
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Next: Add Healthcare Provider", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // STEP 2: Healthcare Provider Setup
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Add Healthcare Provider",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = "Exactly one doctor or provider is linked to this device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.dp, DeepAnchorNavy100),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = doctorName,
                                    onValueChange = { doctorName = it },
                                    label = { Text("Doctor / Provider Full Name *") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = specialty,
                                    onValueChange = { specialty = it },
                                    label = { Text("Clinical Specialty") },
                                    placeholder = { Text("e.g. Cardiology & Vascular Medicine") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = clinicName,
                                    onValueChange = { clinicName = it },
                                    label = { Text("Clinic or Hospital Name") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    label = { Text("Clinic Emergency Contact Phone") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                                    step = 1
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (doctorName.trim().isBlank()) {
                                        Toast.makeText(context, "Please enter healthcare provider name", Toast.LENGTH_SHORT).show()
                                    } else {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)

                                        val updatedPatient = currentPatient.copy(
                                            patientName = studentName.trim().ifBlank { "Student" },
                                            age = ageText.toIntOrNull() ?: currentPatient.age,
                                            bloodType = bloodType.trim().ifBlank { "O+" },
                                            conditionNotes = conditionNotes.trim().ifBlank { currentPatient.conditionNotes },
                                            emergencyContactName = emergencyContactName.trim().ifBlank { currentPatient.emergencyContactName },
                                            emergencyContactPhone = emergencyContactPhone.trim().ifBlank { currentPatient.emergencyContactPhone }
                                        )

                                        val updatedDoctor = currentDoctor.copy(
                                            doctorName = doctorName.trim().ifBlank { "Doctor" },
                                            specialty = specialty.trim().ifBlank { currentDoctor.specialty },
                                            clinicName = clinicName.trim().ifBlank { currentDoctor.clinicName },
                                            contactPhone = contactPhone.trim().ifBlank { currentDoctor.contactPhone }
                                        )

                                        repository.completeOnboarding(updatedPatient, updatedDoctor)
                                        Toast.makeText(context, "Welcome to HeartSync! ✨", Toast.LENGTH_SHORT).show()
                                        onOnboardingFinished()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                                modifier = Modifier
                                    .weight(2f)
                                    .height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
