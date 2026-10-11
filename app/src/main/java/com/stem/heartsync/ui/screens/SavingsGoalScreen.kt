package com.stem.heartsync.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stem.heartsync.audio.HeartSyncAudioManager
import com.stem.heartsync.data.model.SavingsGoal
import com.stem.heartsync.data.model.SavingsTransaction
import com.stem.heartsync.data.repository.HeartSyncRepository
import com.stem.heartsync.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun SavingsGoalScreen(
    repository: HeartSyncRepository,
    audioManager: HeartSyncAudioManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goals by repository.savingsGoals.collectAsState()
    val transactions by repository.savingsTransactions.collectAsState()
    val prefs by repository.userPreferences.collectAsState()

    val currencyFormatter = remember { DecimalFormat("#,##0.00") }
    val dateDisplayFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    // Dialog state
    var selectedGoalForManage by remember { mutableStateOf<SavingsGoal?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }

    // Pagination for general saving records
    val pageSize = 5
    var currentTransactionPage by remember { mutableIntStateOf(0) }
    val totalTransactionPages = remember(transactions) {
        if (transactions.isEmpty()) 1 else ceil(transactions.size / pageSize.toDouble()).toInt()
    }
    val pagedTransactions = remember(transactions, currentTransactionPage) {
        transactions.drop(currentTransactionPage * pageSize).take(pageSize)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Header with "+ New Goal"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Medical Savings & Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                    Text(
                        text = "${goals.size} active funds configured",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepAnchorNavy600
                    )
                }

                Button(
                    onClick = {
                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                        showCreateGoalDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Goal Cards: Each record is one card
        items(goals, key = { it.id }) { goal ->
            SavingsGoalItemCard(
                goal = goal,
                currencyFormatter = currencyFormatter,
                onPiggybankClick = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, prefs.classModeEnabled, prefs.sfxEnabled)
                    selectedGoalForManage = goal
                },
                onQuickAdd = { amount ->
                    audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, prefs.classModeEnabled, prefs.sfxEnabled)
                    repository.adjustSavings(goal.id, amount, "Quick Add ₱${amount.toInt()}")
                    Toast.makeText(context, "Added ₱${amount.toInt()} to ${goal.goalName}! ✨", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Paginated Savings Records Section
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
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DeepAnchorNavy100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = DeepAnchorNavy800,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "General Saving Records",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepAnchorNavy900
                                )
                                Text(
                                    text = "${transactions.size} total entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepAnchorNavy600
                                )
                            }
                        }

                        // Page indicator badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurgicalWhite,
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Page ${currentTransactionPage + 1} of $totalTransactionPages",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No savings adjustments logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepAnchorNavy600.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            pagedTransactions.forEach { tx ->
                                val isAdd = tx.type == "ADD"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = PaperBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isAdd) SoftSkyCard else DeductionRedContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isAdd) Icons.Default.Add else Icons.Default.Remove,
                                                    contentDescription = null,
                                                    tint = if (isAdd) ElectricBlue else DeductionRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                if (tx.goalName.isNotBlank()) {
                                                    Text(
                                                        text = tx.goalName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DeepAnchorNavy900
                                                    )
                                                }
                                                Text(
                                                    text = if (tx.note.isNotBlank()) tx.note else (if (isAdd) "Deposit" else "Expense"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DeepAnchorNavy800
                                                )
                                                Text(
                                                    text = dateDisplayFormatter.format(Date(tx.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepAnchorNavy600.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${if (isAdd) "+" else "-"}₱${currencyFormatter.format(tx.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdd) ElectricBlue else DeductionRed
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pagination Navigation Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentTransactionPage > 0) {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                                        currentTransactionPage--
                                    }
                                },
                                enabled = currentTransactionPage > 0,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous", fontSize = 12.sp)
                            }

                            Text(
                                text = "${currentTransactionPage + 1} / $totalTransactionPages",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepAnchorNavy600
                            )

                            OutlinedButton(
                                onClick = {
                                    if (currentTransactionPage < totalTransactionPages - 1) {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_TAB_SWITCH, prefs.classModeEnabled, prefs.sfxEnabled)
                                        currentTransactionPage++
                                    }
                                },
                                enabled = currentTransactionPage < totalTransactionPages - 1,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Next", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Manage Goal Modal (Piggybank click)
    selectedGoalForManage?.let { goal ->
        ManageSavingsGoalDialog(
            goal = goal,
            onDismiss = { selectedGoalForManage = null },
            onSaveGoal = { updatedGoal ->
                repository.saveSavingsGoal(updatedGoal)
                Toast.makeText(context, "Goal settings updated! ✨", Toast.LENGTH_SHORT).show()
                selectedGoalForManage = null
            },
            onDeleteGoal = {
                repository.deleteSavingsGoal(goal.id)
                Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show()
                selectedGoalForManage = null
            },
            onApplyDelta = { delta, note ->
                repository.adjustSavings(goal.id, delta, note)
                val msg = if (delta >= 0) "Added ₱${delta.toInt()} deposit" else "Deducted ₱${abs(delta).toInt()}"
                Toast.makeText(context, "$msg for ${goal.goalName}", Toast.LENGTH_SHORT).show()
                selectedGoalForManage = null
            },
            audioManager = audioManager,
            classModeEnabled = prefs.classModeEnabled,
            sfxEnabled = prefs.sfxEnabled
        )
    }

    // Create New Goal Modal
    if (showCreateGoalDialog) {
        CreateSavingsGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onCreate = { newGoal ->
                repository.saveSavingsGoal(newGoal)
                Toast.makeText(context, "New savings goal created! 🎉", Toast.LENGTH_SHORT).show()
                showCreateGoalDialog = false
            },
            audioManager = audioManager,
            classModeEnabled = prefs.classModeEnabled,
            sfxEnabled = prefs.sfxEnabled
        )
    }
}

// ---------------------------------------------------------------------------------
// Sub-Components
// ---------------------------------------------------------------------------------

@Composable
private fun SavingsGoalItemCard(
    goal: SavingsGoal,
    currencyFormatter: DecimalFormat,
    onPiggybankClick: () -> Unit,
    onQuickAdd: (Double) -> Unit
) {
    val progressRatio = if (goal.targetAmount > 0) {
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 500),
        label = "progress_${goal.id}"
    )

    val daysLeft = remember(goal.targetDate) { calculateDaysLeft(goal.targetDate) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DeepAnchorNavy800,
        border = BorderStroke(1.dp, DeepAnchorNavy600.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Pink Title & Labeled Deadline on Left, Piggybank Icon on the RIGHT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.goalName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RosePulse300,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Labeled deadline day left (optional)
                    if (daysLeft != null) {
                        val badgeBorder = if (daysLeft < 0) DeductionRed.copy(alpha = 0.7f) else RosePulse300.copy(alpha = 0.4f)
                        val badgeTextColor = if (daysLeft < 0) DeductionRed else RosePulse300
                        val badgeLabel = when {
                            daysLeft > 1 -> "⏳ $daysLeft days left"
                            daysLeft == 1L -> "⏳ 1 day left"
                            daysLeft == 0L -> "⚠️ Due today"
                            else -> "⚠️ ${abs(daysLeft)} days overdue"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepAnchorNavy900,
                            border = BorderStroke(1.dp, badgeBorder),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Clickable Piggybank Icon on the RIGHT
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DeepAnchorNavy900)
                        .border(1.dp, RosePulse300.copy(alpha = 0.6f), CircleShape)
                        .clickable(onClick = onPiggybankClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Manage Goal (Piggybank)",
                        tint = RosePulse300,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Row: Current vs Target (White text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "₱${currencyFormatter.format(goal.currentAmount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SurgicalWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Target: ₱${currencyFormatter.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SurgicalWhite.copy(alpha = 0.75f),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = "${(progressRatio * 100).toInt()}% Reached",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RosePulse300,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pink Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = RosePulse300,
                trackColor = DeepAnchorNavy900
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = DeepAnchorNavy600.copy(alpha = 0.4f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Add amount below it: Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SurgicalWhite.copy(alpha = 0.8f),
                    maxLines = 1,
                    softWrap = false
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val addAmounts = listOf(goal.quickAdd1, goal.quickAdd2, goal.quickAdd3)
                    addAmounts.forEach { amt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DeepAnchorNavy900,
                            border = BorderStroke(1.dp, RosePulse300.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onQuickAdd(amt) }
                        ) {
                            Text(
                                text = "+₱${amt.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = RosePulse300,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageSavingsGoalDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSaveGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: () -> Unit,
    onApplyDelta: (Double, String) -> Unit,
    audioManager: HeartSyncAudioManager,
    classModeEnabled: Boolean,
    sfxEnabled: Boolean
) {
    var editGoalName by remember { mutableStateOf(goal.goalName) }
    var editTargetAmount by remember { mutableStateOf(goal.targetAmount.toInt().toString()) }
    var editTargetDate by remember { mutableStateOf(goal.targetDate ?: "") }
    var editQ1 by remember { mutableStateOf(goal.quickAdd1.toInt().toString()) }
    var editQ2 by remember { mutableStateOf(goal.quickAdd2.toInt().toString()) }
    var editQ3 by remember { mutableStateOf(goal.quickAdd3.toInt().toString()) }

    var customDeltaText by remember { mutableStateOf("") }
    var customNoteText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurgicalWhite,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SoftSkyCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Customize ${goal.goalName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy900
                    )
                    Text(
                        text = "Current balance: ₱${goal.currentAmount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepAnchorNavy600
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Add or Deduct Custom Amount
                Text(
                    text = "Deposit or Deduct Amount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )

                OutlinedTextField(
                    value = customDeltaText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) customDeltaText = it },
                    label = { Text("Amount (₱)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customNoteText,
                    onValueChange = { customNoteText = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val amt = customDeltaText.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, classModeEnabled, sfxEnabled)
                                onApplyDelta(amt, customNoteText.ifBlank { "Custom Deposit" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Deposit", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amt = customDeltaText.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                onApplyDelta(-amt, customNoteText.ifBlank { "Expense Deduction" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeductionRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("- Deduct", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = DeepAnchorNavy100, thickness = 1.dp)

                // Section 2: Customize Fixed Quick Add Presets
                Text(
                    text = "Customize 3 Quick Add Buttons (₱)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editQ1,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editQ1 = it },
                        label = { Text("#1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editQ2,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editQ2 = it },
                        label = { Text("#2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editQ3,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editQ3 = it },
                        label = { Text("#3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = DeepAnchorNavy100, thickness = 1.dp)

                // Section 3: Goal Details & Deadline
                Text(
                    text = "Goal Details & Optional Deadline",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )

                OutlinedTextField(
                    value = editGoalName,
                    onValueChange = { editGoalName = it },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editTargetAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editTargetAmount = it },
                    label = { Text("Target Goal (₱)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Calendar Selector Field for Target Deadline Date
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target Deadline Date (Optional)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy800
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (editTargetDate.isNotBlank()) SoftSkyCard else PaperBackground,
                        border = BorderStroke(
                            1.dp,
                            if (editTargetDate.isNotBlank()) ElectricBlue.copy(alpha = 0.5f) else DeepAnchorNavy100
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                showDatePicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (editTargetDate.isNotBlank()) ElectricBlue else DeepAnchorNavy100.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Deadline Date",
                                        tint = if (editTargetDate.isNotBlank()) SurgicalWhite else DeepAnchorNavy600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    if (editTargetDate.isNotBlank()) {
                                        Text(
                                            text = formatFriendlyDate(editTargetDate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepAnchorNavy900
                                        )
                                        val days = calculateDaysLeft(editTargetDate)
                                        val daysText = when {
                                            days == null -> editTargetDate
                                            days > 1 -> "⏳ $days days left"
                                            days == 1L -> "⏳ 1 day left"
                                            days == 0L -> "⚠️ Due today"
                                            else -> "⚠️ ${abs(days)} days overdue"
                                        }
                                        Text(
                                            text = daysText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (days != null && days < 0) DeductionRed else ElectricBlue
                                        )
                                    } else {
                                        Text(
                                            text = "Pick target deadline",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DeepAnchorNavy600.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "Calendar selector (no typing needed)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepAnchorNavy600.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            if (editTargetDate.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                        editTargetDate = ""
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear deadline date",
                                        tint = DeepAnchorNavy600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = DeepAnchorNavy600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, classModeEnabled, sfxEnabled)
                    val target = editTargetAmount.toDoubleOrNull() ?: goal.targetAmount
                    val q1 = editQ1.toDoubleOrNull() ?: 20.0
                    val q2 = editQ2.toDoubleOrNull() ?: 50.0
                    val q3 = editQ3.toDoubleOrNull() ?: 100.0
                    val updated = goal.copy(
                        goalName = editGoalName.ifBlank { goal.goalName },
                        targetAmount = if (target > 0) target else 500.0,
                        targetDate = editTargetDate.trim().ifBlank { null },
                        quickAdd1 = q1,
                        quickAdd2 = q2,
                        quickAdd3 = q3
                    )
                    onSaveGoal(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDeleteGoal,
                    colors = ButtonDefaults.textButtonColors(contentColor = DeductionRed)
                ) {
                    Text("Delete Goal", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = DeepAnchorNavy600)
                }
            }
        }
    )

    if (showDatePicker) {
        SavingsDatePickerModal(
            initialDateKey = editTargetDate,
            onDateSelected = { selected ->
                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                editTargetDate = selected
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun CreateSavingsGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (SavingsGoal) -> Unit,
    audioManager: HeartSyncAudioManager,
    classModeEnabled: Boolean,
    sfxEnabled: Boolean
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("500") }
    var targetDate by remember { mutableStateOf("") }
    var q1 by remember { mutableStateOf("20") }
    var q2 by remember { mutableStateOf("50") }
    var q3 by remember { mutableStateOf("100") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurgicalWhite,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RosePulse100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CardiacCrimson600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New Savings Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("What are you saving for?") },
                    placeholder = { Text("e.g. Heart Monitor, Medications") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetAmount = it },
                    label = { Text("Target Amount (₱)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Calendar Selector Field for Target Deadline Date
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target Deadline Date (Optional)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepAnchorNavy800
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (targetDate.isNotBlank()) SoftSkyCard else PaperBackground,
                        border = BorderStroke(
                            1.dp,
                            if (targetDate.isNotBlank()) ElectricBlue.copy(alpha = 0.5f) else DeepAnchorNavy100
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                showDatePicker = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (targetDate.isNotBlank()) ElectricBlue else DeepAnchorNavy100.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Deadline Date",
                                        tint = if (targetDate.isNotBlank()) SurgicalWhite else DeepAnchorNavy600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    if (targetDate.isNotBlank()) {
                                        Text(
                                            text = formatFriendlyDate(targetDate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepAnchorNavy900
                                        )
                                        val days = calculateDaysLeft(targetDate)
                                        val daysText = when {
                                            days == null -> targetDate
                                            days > 1 -> "⏳ $days days left"
                                            days == 1L -> "⏳ 1 day left"
                                            days == 0L -> "⚠️ Due today"
                                            else -> "⚠️ ${abs(days)} days overdue"
                                        }
                                        Text(
                                            text = daysText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (days != null && days < 0) DeductionRed else ElectricBlue
                                        )
                                    } else {
                                        Text(
                                            text = "Pick target deadline",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DeepAnchorNavy600.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "Calendar selector (no typing needed)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepAnchorNavy600.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }

                            if (targetDate.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                                        targetDate = ""
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear deadline date",
                                        tint = DeepAnchorNavy600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = DeepAnchorNavy600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Quick Add Presets (₱)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepAnchorNavy900
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = q1,
                        onValueChange = { if (it.all { c -> c.isDigit() }) q1 = it },
                        label = { Text("#1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = q2,
                        onValueChange = { if (it.all { c -> c.isDigit() }) q2 = it },
                        label = { Text("#2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = q3,
                        onValueChange = { if (it.all { c -> c.isDigit() }) q3 = it },
                        label = { Text("#3") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalName.isNotBlank()) {
                        audioManager.playSfx(HeartSyncAudioManager.SFX_SUCCESS, classModeEnabled, sfxEnabled)
                        val target = targetAmount.toDoubleOrNull() ?: 500.0
                        val quick1 = q1.toDoubleOrNull() ?: 20.0
                        val quick2 = q2.toDoubleOrNull() ?: 50.0
                        val quick3 = q3.toDoubleOrNull() ?: 100.0
                        val newGoal = SavingsGoal(
                            id = 0L,
                            goalName = goalName.trim(),
                            targetAmount = if (target > 0) target else 500.0,
                            currentAmount = 0.0,
                            currencySymbol = "₱",
                            targetDate = targetDate.trim().ifBlank { null },
                            quickAdd1 = quick1,
                            quickAdd2 = quick2,
                            quickAdd3 = quick3,
                            createdAt = System.currentTimeMillis()
                        )
                        onCreate(newGoal)
                    }
                },
                enabled = goalName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CardiacCrimson600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Goal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeepAnchorNavy600)
            }
        }
    )

    if (showDatePicker) {
        SavingsDatePickerModal(
            initialDateKey = targetDate,
            onDateSelected = { selected ->
                audioManager.playSfx(HeartSyncAudioManager.SFX_CLICK, classModeEnabled, sfxEnabled)
                targetDate = selected
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

private fun calculateDaysLeft(targetDateStr: String?): Long? {
    if (targetDateStr.isNullOrBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val target = sdf.parse(targetDateStr) ?: return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val diffMillis = target.time - today.time
        diffMillis / (1000 * 60 * 60 * 24)
    } catch (_: Exception) {
        null
    }
}

private fun formatFriendlyDate(dateKey: String): String {
    if (dateKey.isBlank()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateKey) ?: return dateKey
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        dateKey
    }
}

private fun formatUtcMillisToDateKey(utcMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(utcMillis))
}

private fun parseDateKeyToUtcMillis(dateKey: String): Long? {
    if (dateKey.isBlank()) return null
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        formatter.parse(dateKey)?.time
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsDatePickerModal(
    initialDateKey: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDateKey) {
        parseDateKeyToUtcMillis(initialDateKey) ?: System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(formatUtcMillisToDateKey(millis))
                    }
                    onDismiss()
                }
            ) {
                Text("Select Date", fontWeight = FontWeight.Bold, color = ElectricBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DeepAnchorNavy600)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = SurgicalWhite
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = SurgicalWhite,
                selectedDayContainerColor = ElectricBlue,
                selectedDayContentColor = SurgicalWhite,
                todayDateBorderColor = ElectricBlue,
                todayContentColor = ElectricBlue,
                yearContentColor = DeepAnchorNavy900,
                currentYearContentColor = ElectricBlue,
                selectedYearContainerColor = ElectricBlue,
                selectedYearContentColor = SurgicalWhite
            )
        )
    }
}
