package com.stem.heartsync.ui.theme

import androidx.compose.ui.graphics.Color

// STRICT PALETTE: Red, Pink, Blue, White only (Zero green, zero yellow, zero violet)

// 1. Reds
val CardiacCrimson900 = Color(0xFF8A0014)
val CardiacCrimson600 = Color(0xFFBA1A2E) // Primary Crimson Red
val CardiacCrimson400 = Color(0xFFDE374A)
val DeductionRed = Color(0xFFE11D48)
val DeductionRedContainer = Color(0xFFFFF0F3)

// 2. Pinks
val RosePulse500 = Color(0xFFE84370)
val RosePulse300 = Color(0xFFFF8FA3) // Secondary Rose Pink
val RosePulse100 = Color(0xFFFDE8EC)
val RosePulse50 = Color(0xFFFFF1F4) // Soft Rose Tint
val SoftBlushCard = Color(0xFFFFF0F3) // Soft Rose Container

// 3. Blues & Navies
val DeepAnchorNavy900 = Color(0xFF0A1128) // Deep Navy Text & Accents
val DeepAnchorNavy800 = Color(0xFF1C2541) // Rich Navy Card Header
val DeepAnchorNavy600 = Color(0xFF3A506B) // Muted Anchor Navy
val DeepAnchorNavy100 = Color(0xFFE8EEF5) // Soft Slate Blue Pill
val ElectricBlue = Color(0xFF2563EB)      // Vibrant Blue Accent
val SoftSkyCard = Color(0xFFEFF5FF)       // Soft Ice/Sky Blue Container

// 4. Whites
val SurgicalWhite = Color(0xFFFFFFFF)
val PaperBackground = Color(0xFFF8F9FB)   // Clean Porcelain
val WarmPorcelain = Color(0xFFF8F9FB)

// Aliases mapped strictly to Blue and Pink/Red (Removing green, yellow, violet)
val SoftMintCard = SoftSkyCard            // Replaced green mint with soft sky blue
val SoftHoneyCard = SoftBlushCard         // Replaced yellow honey with soft rose pink
val SoftLavenderCard = SoftSkyCard        // Replaced violet with soft sky blue

val VitalityMint = ElectricBlue           // Replaced green with electric blue
val VitalityMintContainer = SoftSkyCard
val WarningAmber = CardiacCrimson600      // Replaced yellow with primary crimson
val WarningAmberContainer = RosePulse100
val AdditionGreen = ElectricBlue          // Replaced green with electric blue
val AdditionGreenContainer = SoftSkyCard
