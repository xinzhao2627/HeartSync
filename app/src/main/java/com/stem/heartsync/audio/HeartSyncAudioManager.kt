package com.stem.heartsync.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

class HeartSyncAudioManager private constructor(private val context: Context) {

    companion object {
        const val SFX_CLICK = 1
        const val SFX_TAB_SWITCH = 2
        const val SFX_SUCCESS = 3
        const val SFX_HEARTBEAT = 4

        @Volatile
        private var INSTANCE: HeartSyncAudioManager? = null

        fun getInstance(context: Context): HeartSyncAudioManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeartSyncAudioManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var soundPool: SoundPool? = null
    private val soundIdMap = mutableMapOf<Int, Int>()
    private var isInitialized = false

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            // Pre-generate tactile, blocky, mechanical woodblock sounds in cache directory
            val sampleRate = 44100
            soundIdMap[SFX_CLICK] = generateBlockyClick(sampleRate)
            soundIdMap[SFX_TAB_SWITCH] = generateBlockyTab(sampleRate)
            soundIdMap[SFX_SUCCESS] = generateBlockySuccess(sampleRate)
            soundIdMap[SFX_HEARTBEAT] = generateBlockyHeartbeat(sampleRate)

            isInitialized = true
        } catch (e: Exception) {
            // Audio engine initialization fallback
        }
    }

    fun playSfx(sfxType: Int, isClassMode: Boolean, isSfxEnabled: Boolean) {
        // Trigger subtle tactile haptic feedback
        performHaptic(sfxType)

        // Class Mode strictly silences audible sounds to avoid disturbing lectures
        if (isClassMode || !isSfxEnabled) return

        val soundId = soundIdMap[sfxType] ?: return
        if (soundId > 0) {
            soundPool?.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    fun triggerHeartbeatVibration() {
        val pattern = longArrayOf(0, 40, 80, 50)
        vibratePattern(pattern)
    }

    private fun performHaptic(sfxType: Int) {
        val pattern = when (sfxType) {
            SFX_CLICK -> longArrayOf(0, 12)
            SFX_TAB_SWITCH -> longArrayOf(0, 10)
            SFX_SUCCESS -> longArrayOf(0, 15, 25, 20)
            SFX_HEARTBEAT -> longArrayOf(0, 35, 50, 45)
            else -> longArrayOf(0, 12)
        }
        vibratePattern(pattern)
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Vibrator fallback
        }
    }

    // 1. Tactile block click (woodblock/mechanical switch "thock" with pitch sweep & organic harmonics)
    private fun generateBlockyClick(sampleRate: Int): Int {
        val durationMs = 28
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Downward pitch sweep for mechanical woodblock impact (420Hz -> 180Hz)
            val freq = 180.0 + 240.0 * Math.exp(-t / 0.006)
            phase += 2.0 * Math.PI * freq / sampleRate

            // Dual resonant wood harmonics
            val raw = sin(phase) * 0.65 + sin(phase * 2.35) * 0.35
            // Soft saturation for tactile block body
            val blocky = Math.tanh(raw * 2.0)
            // Fast organic exponential decay
            val env = Math.exp(-t / 0.008)
            val attack = Math.min(1.0, t / 0.0005)
            val release = if (i > numSamples - 40) (numSamples - i) / 40.0 else 1.0

            sample[i] = (blocky * env * attack * release * 26000.0).toInt().toShort()
        }
        return writeAndLoadSoundPoolFile("sfx_blocky_click.wav", sample, sampleRate)
    }

    // 2. Hollow tactile block tap for tab & route swiping (320Hz -> 220Hz)
    private fun generateBlockyTab(sampleRate: Int): Int {
        val durationMs = 32
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 220.0 + 100.0 * Math.exp(-t / 0.008)
            phase += 2.0 * Math.PI * freq / sampleRate

            val raw = sin(phase) * 0.70 + sin(phase * 2.20) * 0.30
            val blocky = Math.tanh(raw * 1.6)
            val env = Math.exp(-t / 0.010)
            val attack = Math.min(1.0, t / 0.0006)
            val release = if (i > numSamples - 40) (numSamples - i) / 40.0 else 1.0

            sample[i] = (blocky * env * attack * release * 24000.0).toInt().toShort()
        }
        return writeAndLoadSoundPoolFile("sfx_blocky_tab.wav", sample, sampleRate)
    }

    // 3. Cheerful wooden marimba block strike (G4 -> D5 two-block sequence)
    private fun generateBlockySuccess(sampleRate: Int): Int {
        val durationMs = 110
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        var phase = 0.0
        val splitSample = (42 * sampleRate) / 1000

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteTime = if (i < splitSample) t else (i - splitSample).toDouble() / sampleRate
            val baseFreq = if (i < splitSample) 392.0 else 587.33

            val freq = baseFreq + 120.0 * Math.exp(-noteTime / 0.006)
            phase += 2.0 * Math.PI * freq / sampleRate

            val raw = sin(phase) * 0.70 + sin(phase * 2.30) * 0.30
            val blocky = Math.tanh(raw * 1.8)
            val env = Math.exp(-noteTime / 0.018)
            val attack = Math.min(1.0, noteTime / 0.0008)
            val release = if (i > numSamples - 50) (numSamples - i) / 50.0 else 1.0

            sample[i] = (blocky * env * attack * release * 25000.0).toInt().toShort()
        }
        return writeAndLoadSoundPoolFile("sfx_blocky_success.wav", sample, sampleRate)
    }

    // 4. Muffled low-frequency tactile block thud for heartbeat pulse (75Hz)
    private fun generateBlockyHeartbeat(sampleRate: Int): Int {
        val durationMs = 80
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 75.0 + 35.0 * Math.exp(-t / 0.015)
            phase += 2.0 * Math.PI * freq / sampleRate

            val raw = sin(phase)
            val blocky = Math.tanh(raw * 2.2)
            val env = Math.exp(-t / 0.022)
            val attack = Math.min(1.0, t / 0.002)
            val release = if (i > numSamples - 40) (numSamples - i) / 40.0 else 1.0

            sample[i] = (blocky * env * attack * release * 28000.0).toInt().toShort()
        }
        return writeAndLoadSoundPoolFile("sfx_blocky_heartbeat.wav", sample, sampleRate)
    }

    private fun writeAndLoadSoundPoolFile(fileName: String, samples: ShortArray, sampleRate: Int): Int {
        val file = File(context.cacheDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                val totalAudioLen = samples.size * 2
                val totalDataLen = totalAudioLen + 36
                val byteRate = sampleRate * 2

                val header = ByteArray(44)
                header[0] = 'R'.code.toByte()
                header[1] = 'I'.code.toByte()
                header[2] = 'F'.code.toByte()
                header[3] = 'F'.code.toByte()
                header[4] = (totalDataLen and 0xff).toByte()
                header[5] = ((totalDataLen shr 8) and 0xff).toByte()
                header[6] = ((totalDataLen shr 16) and 0xff).toByte()
                header[7] = ((totalDataLen shr 24) and 0xff).toByte()
                header[8] = 'W'.code.toByte()
                header[9] = 'A'.code.toByte()
                header[10] = 'V'.code.toByte()
                header[11] = 'E'.code.toByte()
                header[12] = 'f'.code.toByte()
                header[13] = 'm'.code.toByte()
                header[14] = 't'.code.toByte()
                header[15] = ' '.code.toByte()
                header[16] = 16
                header[17] = 0
                header[18] = 0
                header[19] = 0
                header[20] = 1 // PCM
                header[21] = 0
                header[22] = 1 // Mono
                header[23] = 0
                header[24] = (sampleRate and 0xff).toByte()
                header[25] = ((sampleRate shr 8) and 0xff).toByte()
                header[26] = ((sampleRate shr 16) and 0xff).toByte()
                header[27] = ((sampleRate shr 24) and 0xff).toByte()
                header[28] = (byteRate and 0xff).toByte()
                header[29] = ((byteRate shr 8) and 0xff).toByte()
                header[30] = ((byteRate shr 16) and 0xff).toByte()
                header[31] = ((byteRate shr 24) and 0xff).toByte()
                header[32] = 2 // Block align
                header[33] = 0
                header[34] = 16 // Bits per sample
                header[35] = 0
                header[36] = 'd'.code.toByte()
                header[37] = 'a'.code.toByte()
                header[38] = 't'.code.toByte()
                header[39] = 'a'.code.toByte()
                header[40] = (totalAudioLen and 0xff).toByte()
                header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
                header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
                header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

                out.write(header)
                val buffer = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    buffer[i * 2] = (samples[i].toInt() and 0xff).toByte()
                    buffer[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xff).toByte()
                }
                out.write(buffer)
            }
            return soundPool?.load(file.absolutePath, 1) ?: 0
        } catch (e: Exception) {
            return 0
        }
    }
}
