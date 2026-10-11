package com.stem.heartsync.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.UserRole
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.theme.*

@Composable
fun RoleSelectionScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val patient by repository.patientProfile.collectAsState()
    val doctor by repository.doctorProfile.collectAsState()
    val prefs by repository.userPreferences.collectAsState()

    Scaffold(
        containerColor = PaperBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(RosePulse100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = CardiacCrimson600,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HeartSync",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepAnchorNavy900
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Continue as",
                style = MaterialTheme.typography.titleMedium,
                color = DeepAnchorNavy600
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurgicalWhite,
                border = BorderStroke(1.5.dp, RosePulse300.copy(alpha = 0.5f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                        onRoleSelected(UserRole.PATIENT)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(RosePulse100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = CardiacCrimson600,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Patient",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepAnchorNavy900
                        )
                        Text(
                            text = patient.patientName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = CardiacCrimson600,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurgicalWhite,
                border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.35f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                        onRoleSelected(UserRole.DOCTOR)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftSkyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Healthcare Provider",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepAnchorNavy900
                        )
                        Text(
                            text = doctor.doctorName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepAnchorNavy600
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
