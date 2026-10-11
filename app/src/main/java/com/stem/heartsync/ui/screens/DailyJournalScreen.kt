package com.stem.heartsync.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.DailyJournalEntry
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.components.MarkdownVisualTransformation
import com.stem.heartsync.ui.components.RichEditorActions
import com.stem.heartsync.ui.components.RichMarkdownPreview
import com.stem.heartsync.ui.components.parseMarkdownToAnnotatedString
import com.stem.heartsync.ui.theme.*

enum class JournalCabinetMode {
    THANKFUL,
    STRUGGLES
}

@Composable
fun DailyJournalScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    onEditingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayKey by repository.currentDateKey.collectAsState()
    val prefs by repository.userPreferences.collectAsState()
    val journalEntries by repository.journalEntries.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var activeEntryDate by remember { mutableStateOf(todayKey) }
    var editingEntryId by remember { mutableStateOf(0L) }

    var strugglesText by remember { mutableStateOf("") }
    var thankfulText by remember { mutableStateOf("") }
    var moodScore by remember { mutableIntStateOf(3) }
    var heartRateInput by remember { mutableStateOf("72") }

    // Full-page Cabinet Writer state
    var cabinetMode by remember { mutableStateOf<JournalCabinetMode?>(null) }
    var cabinetInitialText by remember { mutableStateOf("") }
    var editorTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing, cabinetMode) {
        onEditingChanged(isEditing || cabinetMode != null)
    }

    fun openEditorForDate(dateKey: String) {
        val entry = journalEntries.find { it.entryDate == dateKey }
            ?: repository.getJournalForDate(dateKey)

        activeEntryDate = dateKey
        if (entry != null) {
            editingEntryId = entry.id
            strugglesText = entry.struggles
            thankfulText = entry.thankfulFor
            moodScore = entry.moodScore
            heartRateInput = entry.heartRate?.toString() ?: "72"
        } else {
            editingEntryId = 0L
            strugglesText = ""
            thankfulText = ""
            moodScore = 3
            heartRateInput = "72"
        }
        cabinetMode = null
        isEditing = true
    }

    fun saveCurrentEntry() {
        val hr = heartRateInput.toIntOrNull()
        val entry = DailyJournalEntry(
            id = editingEntryId,
            entryDate = activeEntryDate,
            moodScore = moodScore,
            struggles = strugglesText,
            thankfulFor = thankfulText,
            heartRate = hr,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveJournal(entry)
    }

    fun openCabinet(mode: JournalCabinetMode) {
        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
        cabinetMode = mode
        isPreviewMode = false
        val initial = if (mode == JournalCabinetMode.THANKFUL) thankfulText else strugglesText
        cabinetInitialText = initial
        editorTextFieldValue = TextFieldValue(
            text = initial,
            selection = TextRange(initial.length)
        )
    }

    fun requestCloseCabinet() {
        if (editorTextFieldValue.text != cabinetInitialText) {
            showUnsavedDialog = true
        } else {
            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
            cabinetMode = null
        }
    }

    fun saveCabinetContent() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
        if (cabinetMode == JournalCabinetMode.THANKFUL) {
            thankfulText = editorTextFieldValue.text
        } else if (cabinetMode == JournalCabinetMode.STRUGGLES) {
            strugglesText = editorTextFieldValue.text
        }
        cabinetInitialText = editorTextFieldValue.text
        saveCurrentEntry()
        Toast.makeText(context, "Reflection saved! ❤️", Toast.LENGTH_SHORT).show()
        cabinetMode = null
    }

    fun toggleBold() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleBold(editorTextFieldValue)
    }

    fun toggleItalic() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleItalic(editorTextFieldValue)
    }

    fun toggleHeading() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleHeading(editorTextFieldValue)
    }

    fun toggleBullet() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleBullet(editorTextFieldValue)
    }

    fun toggleNumbered() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleNumbered(editorTextFieldValue)
    }

    fun toggleQuote() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleQuote(editorTextFieldValue)
    }

    fun toggleChecklist() {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        editorTextFieldValue = RichEditorActions.toggleChecklist(editorTextFieldValue)
    }

    // Intercept hardware/gesture back press hierarchically
    BackHandler(enabled = cabinetMode != null) {
        requestCloseCabinet()
    }

    BackHandler(enabled = cabinetMode == null && isEditing) {
        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
        saveCurrentEntry()
        isEditing = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground)
    ) {
        Crossfade(targetState = isEditing, label = "journal_screen_crossfade") { editing ->
            if (!editing) {
                // =========================================================================
                // VIEW A: JOURNAL NOTES LIST (FEED OF REFLECTIONS + FAB)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PaperBackground)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = SoftBlushCard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(CardiacCrimson600.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = CardiacCrimson600,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Heart Reflection Diary",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepAnchorNavy900
                                        )
                                        Text(
                                            text = "${journalEntries.size} reflections saved locally",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DeepAnchorNavy600
                                        )
                                    }
                                }
                            }
                        }

                        if (journalEntries.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = SurgicalWhite,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(RosePulse100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = CardiacCrimson600,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Your heart diary is waiting",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepAnchorNavy900
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap Write Reflection below to record your daily struggles and moments of gratitude.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DeepAnchorNavy600,
                                            lineHeight = 20.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(journalEntries) { entry ->
                                JournalEntryCard(
                                    entry = entry,
                                    onClick = {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                        openEditorForDate(entry.entryDate)
                                    }
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                            openEditorForDate(todayKey)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = CardiacCrimson600,
                        contentColor = SurgicalWhite
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reflection")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Write Reflection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // =========================================================================
                // VIEW B: REFLECTION HUB (HEART CHECK-IN & THANKFUL / STRUGGLES CARDS)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PaperBackground)
                ) {
                    // Top Bar with Back, Title, and Save Icon
                    Surface(
                        color = SurgicalWhite,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                saveCurrentEntry()
                                isEditing = false
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DeepAnchorNavy900
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Daily Reflection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = activeEntryDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                            }

                            // Balanced spacer keeping the title centered
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }

                    // Content column with Check-in and Cabinet Launcher Cards
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Heart Mood & Pulse Check-in
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.dp, DeepAnchorNavy100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "How does your heart feel today?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Mood selector row
                                val moods = listOf(
                                    1 to ("🌧️" to "Heavy"),
                                    2 to ("⚡" to "Flutters"),
                                    3 to ("💖" to "Calm"),
                                    4 to ("🌿" to "Smooth"),
                                    5 to ("✨" to "Energetic")
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    moods.forEach { (score, data) ->
                                        val isSelected = moodScore == score
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) RosePulse100 else PaperBackground,
                                            border = if (isSelected) BorderStroke(1.dp, CardiacCrimson600) else null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                                    moodScore = score
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = data.first, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = data.second,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) CardiacCrimson900 else DeepAnchorNavy600
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Resting Pulse",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600
                                    )
                                    OutlinedTextField(
                                        value = heartRateInput,
                                        onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) heartRateInput = it },
                                        suffix = { Text("bpm", fontSize = 12.sp, color = DeepAnchorNavy600) },
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricBlue,
                                            unfocusedBorderColor = DeepAnchorNavy100
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Thankful Block (Card that pulls up full-page cabinet)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.5.dp, RosePulse300.copy(alpha = 0.5f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    openCabinet(JournalCabinetMode.THANKFUL)
                                }
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
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(RosePulse100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = CardiacCrimson600,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "✨ Moments of Gratitude",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepAnchorNavy900
                                            )
                                            Text(
                                                text = if (thankfulText.isBlank()) "Tap to pull up editor" else "${thankfulText.length} characters",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepAnchorNavy600
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RosePulse50
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "Open Gratitude Cabinet",
                                            tint = CardiacCrimson600,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (thankfulText.isNotBlank()) {
                                    Text(
                                        text = parseMarkdownToAnnotatedString(thankfulText),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy900,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 22.sp
                                    )
                                } else {
                                    Text(
                                        text = "What brought peace, relief, or gratitude to your heart today? Tap to pull up full-page writer...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600.copy(alpha = 0.6f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }

                        // 3. Struggles Block (Card that pulls up full-page cabinet)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.35f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    openCabinet(JournalCabinetMode.STRUGGLES)
                                }
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
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SoftSkyCard),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cloud,
                                                contentDescription = null,
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "🌧️ Today's Honest Struggles",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepAnchorNavy900
                                            )
                                            Text(
                                                text = if (strugglesText.isBlank()) "Tap to pull up editor" else "${strugglesText.length} characters",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepAnchorNavy600
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftSkyCard
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "Open Struggles Cabinet",
                                            tint = ElectricBlue,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (strugglesText.isNotBlank()) {
                                    Text(
                                        text = parseMarkdownToAnnotatedString(strugglesText),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy900,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 22.sp
                                    )
                                } else {
                                    Text(
                                        text = "Did you experience fatigue, palpitation spells, or stress? Tap to pull up full-page writer...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepAnchorNavy600.copy(alpha = 0.6f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // FULL-PAGE CABINET WRITER (SLIDES UP ON PICKING THANKFUL OR STRUGGLES)
        // =========================================================================
        AnimatedVisibility(
            visible = cabinetMode != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(240)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            val mode = cabinetMode ?: JournalCabinetMode.THANKFUL
            val title = if (mode == JournalCabinetMode.THANKFUL) "✨ Moments of Gratitude" else "🌧️ Today's Honest Struggles"
            val placeholder = if (mode == JournalCabinetMode.THANKFUL) {
                "What brought warmth, peace, or relief to your heart today?\n\nWrite freely with complete privacy..."
            } else {
                "Did you feel exhausted, experience palpitation spells, or worry about school?\n\nLet it out safely..."
            }
            val accentColor = if (mode == JournalCabinetMode.THANKFUL) CardiacCrimson600 else ElectricBlue

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurgicalWhite)
            ) {
                // Cabinet Header
                Surface(
                    color = SurgicalWhite,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { requestCloseCabinet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = DeepAnchorNavy900
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepAnchorNavy900
                            )
                            Text(
                                text = activeEntryDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepAnchorNavy600
                            )
                        }

                        // Save button is an icon of save
                        val hasUnsavedChanges = editorTextFieldValue.text != cabinetInitialText
                        IconButton(
                            onClick = { saveCabinetContent() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasUnsavedChanges) accentColor.copy(alpha = 0.16f) else DeepAnchorNavy100.copy(alpha = 0.4f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Reflection",
                                tint = if (hasUnsavedChanges) accentColor else DeepAnchorNavy600.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Formatting Toolbar: Bold, Italic, Heading, Bullet, Numbered, Quote, Checklist, Preview Mode
                Surface(
                    color = PaperBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { toggleBold() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { toggleItalic() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatItalic,
                                contentDescription = "Italic",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { toggleHeading() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Text(
                                text = "H",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = DeepAnchorNavy900
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 4.dp),
                            color = DeepAnchorNavy100
                        )

                        IconButton(
                            onClick = { toggleBullet() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Bullet List",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { toggleNumbered() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = "Numbered List",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { toggleQuote() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "Quote",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { toggleChecklist() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Checklist",
                                tint = DeepAnchorNavy900,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 4.dp),
                            color = DeepAnchorNavy100
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPreviewMode) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            IconButton(
                                onClick = {
                                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                                    isPreviewMode = !isPreviewMode
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = if (isPreviewMode) "Edit Mode" else "Preview Mode",
                                    tint = if (isPreviewMode) accentColor else DeepAnchorNavy900,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DeepAnchorNavy100.copy(alpha = 0.5f), thickness = 1.dp)

                // Full-page Pure White Writing Canvas with live VisualTransformation or Clean Preview
                if (isPreviewMode) {
                    RichMarkdownPreview(
                        content = editorTextFieldValue.text,
                        placeholder = placeholder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    OutlinedTextField(
                        value = editorTextFieldValue,
                        onValueChange = { newValue ->
                            editorTextFieldValue = RichEditorActions.handleNewlineOrValueChange(editorTextFieldValue, newValue)
                        },
                        visualTransformation = MarkdownVisualTransformation(
                            textColor = DeepAnchorNavy900,
                            syntaxColor = DeepAnchorNavy600.copy(alpha = 0.35f),
                            accentColor = accentColor,
                            quoteColor = DeepAnchorNavy800
                        ),
                        placeholder = {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp,
                                    fontSize = 17.sp
                                ),
                                color = DeepAnchorNavy600.copy(alpha = 0.45f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            color = DeepAnchorNavy900
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = SurgicalWhite,
                            unfocusedContainerColor = SurgicalWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Bottom status indicator
                Surface(
                    color = SurgicalWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editorTextFieldValue.text != cabinetInitialText) "Unsaved changes" else "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (editorTextFieldValue.text != cabinetInitialText) CardiacCrimson600 else DeepAnchorNavy600.copy(alpha = 0.6f),
                            fontWeight = if (editorTextFieldValue.text != cabinetInitialText) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${editorTextFieldValue.text.length} characters" + if (isPreviewMode) " • Preview" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepAnchorNavy600.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // UNSAVED CHANGES MODAL DIALOG
        // =========================================================================
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                containerColor = SurgicalWhite,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Unsaved Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                },
                text = {
                    Text(
                        text = "You have unsaved changes in your reflection. Would you like to save or discard them?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepAnchorNavy600
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            saveCabinetContent()
                            showUnsavedDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = SurgicalWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold, color = SurgicalWhite)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                            showUnsavedDialog = false
                            cabinetMode = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeductionRed),
                        border = BorderStroke(1.dp, DeductionRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard", fontWeight = FontWeight.Bold, color = DeductionRed)
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------------
// Sub-Components for Journal Feed
// ---------------------------------------------------------------------------------

@Composable
private fun JournalEntryCard(
    entry: DailyJournalEntry,
    onClick: () -> Unit
) {
    val moodEmoji = when (entry.moodScore) {
        1 -> "🌧️ Heavy / Dizzy"
        2 -> "⚡ Flutters"
        3 -> "💖 Calm & Stable"
        4 -> "🌿 Smooth Rhythm"
        5 -> "✨ Energetic"
        else -> "💖 Calm"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurgicalWhite,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.entryDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RosePulse100
                ) {
                    Text(
                        text = moodEmoji,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CardiacCrimson900,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (entry.thankfulFor.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = "✨", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = parseMarkdownToAnnotatedString(entry.thankfulFor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepAnchorNavy800,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (entry.struggles.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = "🌧️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = parseMarkdownToAnnotatedString(entry.struggles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepAnchorNavy600,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
