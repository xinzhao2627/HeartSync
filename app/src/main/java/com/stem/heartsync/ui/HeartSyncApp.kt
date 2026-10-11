package com.stem.heartsync.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.PatientNavDestination
import com.stem.heartsync.data.model.UserRole
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.components.HeartSyncTopAppBar
import com.stem.heartsync.ui.screens.*
import com.stem.heartsync.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartSyncApp(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager
) {
    val prefs by repository.userPreferences.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var hasSelectedInitialRole by rememberSaveable { mutableStateOf(false) }

    // 5 Swipable Patient Routes
    val patientDestinations = remember {
        listOf(
            PatientNavDestination.HOME,
            PatientNavDestination.ROUTINES,
            PatientNavDestination.JOURNAL,
            PatientNavDestination.SAVINGS,
            PatientNavDestination.SETTINGS
        )
    }

    val patientPagerState = rememberPagerState(pageCount = { patientDestinations.size })
    var isJournalEditing by remember { mutableStateOf(false) }

    // Navigation backstack for seamless back button support
    val navBackStack = remember { mutableStateListOf(PatientNavDestination.HOME) }
    val currentDestination = patientDestinations[patientPagerState.currentPage]

    fun navigateTo(dest: PatientNavDestination) {
        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
        if (navBackStack.lastOrNull() != dest) {
            navBackStack.add(dest)
        }
        val targetIndex = patientDestinations.indexOf(dest)
        if (targetIndex != -1 && targetIndex != patientPagerState.currentPage) {
            coroutineScope.launch {
                patientPagerState.animateScrollToPage(targetIndex)
            }
        }
    }

    fun navigateBack() {
        if (navBackStack.size > 1) {
            audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
            navBackStack.removeAt(navBackStack.lastIndex)
            val prevDest = navBackStack.last()
            val prevIndex = patientDestinations.indexOf(prevDest)
            if (prevIndex != -1 && prevIndex != patientPagerState.currentPage) {
                coroutineScope.launch {
                    patientPagerState.animateScrollToPage(prevIndex)
                }
            }
        } else if (patientPagerState.currentPage != 0) {
            audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
            coroutineScope.launch {
                patientPagerState.animateScrollToPage(0)
            }
        }
    }

    // Keep backstack synchronized with physical horizontal swipe gestures
    LaunchedEffect(patientPagerState) {
        snapshotFlow { patientPagerState.settledPage }
            .collect { settledIndex ->
                val settledDest = patientDestinations[settledIndex]
                if (navBackStack.lastOrNull() != settledDest) {
                    if (navBackStack.size > 1 && navBackStack[navBackStack.size - 2] == settledDest) {
                        navBackStack.removeAt(navBackStack.lastIndex)
                    } else {
                        navBackStack.add(settledDest)
                    }
                }
            }
    }

    // Hardware/Gesture Back press handling:
    // 1. If currently in Doctor Mode, go back to Role Selection
    BackHandler(enabled = hasSelectedInitialRole && prefs.activeRole == UserRole.DOCTOR) {
        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
        hasSelectedInitialRole = false
    }

    // 2. If in Patient Mode and navigated into a subscreen or swiped forward, pop back to previous frame
    BackHandler(enabled = hasSelectedInitialRole && prefs.activeRole == UserRole.PATIENT && (navBackStack.size > 1 || patientPagerState.currentPage != 0)) {
        navigateBack()
    }

    // 3. If in Patient Mode at the root Home frame, go back to Role Selection
    BackHandler(enabled = hasSelectedInitialRole && prefs.activeRole == UserRole.PATIENT && navBackStack.size <= 1 && patientPagerState.currentPage == 0) {
        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
        hasSelectedInitialRole = false
    }

    // Android 13+ Notification Permission Request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    HeartSyncTheme {
        if (!prefs.isOnboardingCompleted) {
            OnboardingScreen(
                repository = repository,
                audioManager = audioManager,
                onOnboardingFinished = {
                    hasSelectedInitialRole = false
                }
            )
        } else if (!hasSelectedInitialRole) {
            RoleSelectionScreen(
                repository = repository,
                audioManager = audioManager,
                onRoleSelected = { selectedRole ->
                    repository.switchRole(selectedRole)
                    hasSelectedInitialRole = true
                }
            )
        } else {
            Scaffold(
                topBar = {
                    HeartSyncTopAppBar(
                        currentRole = prefs.activeRole,
                        currentDestination = currentDestination,
                        onSelectDestination = { dest -> navigateTo(dest) },
                        onSwitchRole = { newRole ->
                            audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                            repository.switchRole(newRole)
                        }
                    )
                },
                bottomBar = {
                    if (hasSelectedInitialRole && prefs.activeRole == UserRole.PATIENT) {
                        Surface(
                            color = SurgicalWhite,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, DeepAnchorNavy100)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                patientDestinations.forEach { dest ->
                                    val isSelected = dest == currentDestination
                                    val icon = when (dest) {
                                        PatientNavDestination.HOME -> Icons.Default.Home
                                        PatientNavDestination.ROUTINES -> Icons.Default.MedicalServices
                                        PatientNavDestination.JOURNAL -> Icons.AutoMirrored.Filled.MenuBook
                                        PatientNavDestination.SAVINGS -> Icons.Default.Savings
                                        PatientNavDestination.SETTINGS -> Icons.Default.Settings
                                    }
                                    val itemColor = if (isSelected) CardiacCrimson600 else DeepAnchorNavy600

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                navigateTo(dest)
                                            }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = dest.label,
                                            tint = itemColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = dest.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = itemColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Main Content Switching
                    Crossfade(targetState = prefs.activeRole, label = "role_crossfade") { role ->
                        when (role) {
                            UserRole.DOCTOR -> {
                                DoctorCockpitScreen(
                                    repository = repository,
                                    audioManager = audioManager,
                                    onSwitchBackToPatient = { repository.switchRole(UserRole.PATIENT) }
                                )
                            }
                            UserRole.PATIENT -> {
                                HorizontalPager(
                                    state = patientPagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    userScrollEnabled = !isJournalEditing
                                ) { pageIndex ->
                                    when (patientDestinations[pageIndex]) {
                                        PatientNavDestination.HOME -> PatientHomeScreen(
                                            repository = repository,
                                            audioManager = audioManager,
                                            onNavigateToRoutines = { navigateTo(PatientNavDestination.ROUTINES) },
                                            onNavigateToJournal = { navigateTo(PatientNavDestination.JOURNAL) },
                                            onNavigateToSavings = { navigateTo(PatientNavDestination.SAVINGS) }
                                        )
                                        PatientNavDestination.ROUTINES -> PatientRoutinesMedsScreen(
                                            repository = repository,
                                            audioManager = audioManager
                                        )
                                        PatientNavDestination.JOURNAL -> DailyJournalScreen(
                                            repository = repository,
                                            audioManager = audioManager,
                                            onEditingChanged = { isJournalEditing = it }
                                        )
                                        PatientNavDestination.SAVINGS -> SavingsGoalScreen(
                                            repository = repository,
                                            audioManager = audioManager
                                        )
                                        PatientNavDestination.SETTINGS -> SettingsScreen(
                                            repository = repository,
                                            audioManager = audioManager,
                                            onNavigateToDoctorCockpit = { repository.switchRole(UserRole.DOCTOR) }
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
