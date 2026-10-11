package com.stem.heartsync.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.data.model.DoctorProfile
import com.stem.heartsync.data.model.PatientProfile
import com.stem.heartsync.data.model.PatientNavDestination
import com.stem.heartsync.data.model.UserRole
import com.stem.heartsync.ui.theme.*

// 1. Google Classroom-style Stream Hero Card (Care Plan)
@Composable
fun ClassroomHeroBanner(
    patient: PatientProfile,
    doctor: DoctorProfile,
    adherencePercent: Int,
    onEmergencyCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepAnchorNavy800
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cardiology Care Plan",
                        style = MaterialTheme.typography.titleLarge,
                        color = SurgicalWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily Regimen & Adherence Protocol",
                        style = MaterialTheme.typography.bodySmall,
                        color = SurgicalWhite.copy(alpha = 0.75f)
                    )
                }

                // Adherence Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (adherencePercent >= 80) ElectricBlue else CardiacCrimson600
                ) {
                    Text(
                        text = "$adherencePercent% Done",
                        color = SurgicalWhite,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = patient.conditionNotes.take(65) + if (patient.conditionNotes.length > 65) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = SurgicalWhite.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = onEmergencyCall,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RosePulse300
                    ),
                    border = BorderStroke(1.dp, RosePulse300),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Emergency Call",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emergency", fontSize = 12.sp)
                }
            }
        }
    }
}

// 2. HeartSync Unified Top Bar with Route Dropdown and Top-Bar Role Switcher
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartSyncTopAppBar(
    currentRole: UserRole,
    currentDestination: PatientNavDestination,
    onSelectDestination: (PatientNavDestination) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (currentRole == UserRole.PATIENT) CardiacCrimson600 else DeepAnchorNavy800),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentRole == UserRole.PATIENT) Icons.Default.Favorite else Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = SurgicalWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "HeartSync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepAnchorNavy900
                    )
                    Text(
                        text = if (currentRole == UserRole.PATIENT) currentDestination.label else "Doctor Station",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepAnchorNavy600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        actions = {
            // Mode Selection in Top Bar (Student vs Doctor)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentRole == UserRole.PATIENT) RosePulse100 else DeepAnchorNavy800,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val next = if (currentRole == UserRole.PATIENT) UserRole.DOCTOR else UserRole.PATIENT
                        onSwitchRole(next)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (currentRole == UserRole.PATIENT) "🎒 Student" else "🩺 Doctor",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentRole == UserRole.PATIENT) CardiacCrimson900 else SurgicalWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Switch Role",
                        tint = if (currentRole == UserRole.PATIENT) CardiacCrimson600 else RosePulse300,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = WarmPorcelain
        )
    )
}

// 3. Google Classroom Role Switcher (Single-Tap KISS)
@Composable
fun RoleSwitcherBar(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val patientSelected = currentRole == UserRole.PATIENT
            val doctorSelected = currentRole == UserRole.DOCTOR

            val patientBg by animateColorAsState(
                if (patientSelected) DeepAnchorNavy800 else Color.Transparent,
                label = "patient_bg"
            )
            val doctorBg by animateColorAsState(
                if (doctorSelected) CardiacCrimson600 else Color.Transparent,
                label = "doctor_bg"
            )

            // Patient View Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(patientBg)
                    .clickable { onRoleSelected(UserRole.PATIENT) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (patientSelected) SurgicalWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Patient View",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (patientSelected) SurgicalWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Healthcare Provider View Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(doctorBg)
                    .clickable { onRoleSelected(UserRole.DOCTOR) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = if (doctorSelected) SurgicalWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Doctor Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (doctorSelected) SurgicalWhite else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


