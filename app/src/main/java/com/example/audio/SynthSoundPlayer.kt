package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.exp

enum class Waveform {
    SINE,
    TRIANGLE,
    SQUARE,
    DETUNED_SAW,
    FM_PLUCK
}

object SynthSoundPlayer {
    private const val SAMPLE_RATE = 22050
    private const val TAG = "SynthSoundPlayer"
    var isSoundEnabled: Boolean = true

    fun playTone(frequency: Double, durationMs: Int) {
        playSynthTone(frequency, durationMs, Waveform.SINE)
    }

    fun playSynthTone(
        frequency: Double,
        durationMs: Int,
        waveform: Waveform,
        pitchBendEndFreq: Double? = null,
        detuning: Double = 0.008,
        envelopeDecay: Double = 5.0
    ) {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@launch
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                var phase2 = 0.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / numSamples

                    // Pitch envelope / bend
                    val f = if (pitchBendEndFreq != null) {
                        frequency + (pitchBendEndFreq - frequency) * progress
                    } else {
                        frequency
                    }

                    phase += f / SAMPLE_RATE
                    phase2 += (f * (1.0 + detuning)) / SAMPLE_RATE

                    // Keep phases bound between 0 and 1
                    phase -= Math.floor(phase)
                    phase2 -= Math.floor(phase2)

                    val sampleVal = when (waveform) {
                        Waveform.SINE -> {
                            sin(2.0 * Math.PI * phase)
                        }
                        Waveform.TRIANGLE -> {
                            2.0 * Math.abs(2.0 * (phase - 0.5)) - 1.0
                        }
                        Waveform.SQUARE -> {
                            if (phase >= 0.5) 0.35 else -0.35
                        }
                        Waveform.DETUNED_SAW -> {
                            val saw1 = 2.0 * (phase - 0.5)
                            val saw2 = 2.0 * (phase2 - 0.5)
                            0.5 * saw1 + 0.5 * saw2
                        }
                        Waveform.FM_PLUCK -> {
                            // Simple 2-operator FM Synthesis (Modulator: 2x freq, Carrier: 1x freq)
                            val modulatorIndex = 2.0 * exp(-6.0 * progress)
                            val mod = sin(2.0 * Math.PI * phase * 2.0) * modulatorIndex
                            sin(2.0 * Math.PI * phase + mod)
                        }
                    }

                    // Amp envelope: exponential decay with quick release fade out
                    val ampEnv = exp(-envelopeDecay * progress) * (1.0 - progress)

                    buffer[i] = (sampleVal * ampEnv * 28000).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                delay(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing synth tone", e)
            }
        }
    }

    fun playTapSuccess(index: Int) {
        if (!isSoundEnabled) return
        // Pentatonic minor retro scale: E, G, A, B, D, E...
        val frequencies = doubleArrayOf(329.63, 392.00, 440.00, 493.88, 587.33, 659.25, 783.99, 880.00)
        val targetFreq = frequencies[index % frequencies.size]
        
        // Stylish synthwave pluck: Quick FM pitch-bend slide from 12% above down to target frequency
        playSynthTone(
            frequency = targetFreq * 1.12,
            durationMs = 160,
            waveform = Waveform.FM_PLUCK,
            pitchBendEndFreq = targetFreq,
            envelopeDecay = 4.2
        )
    }

    fun playComboMilestone(combo: Int) {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            // Root pitch scales up with high combo tiers
            val root = when {
                combo >= 20 -> 392.00 // G4
                combo >= 10 -> 329.63 // E4
                else -> 261.63       // C4
            }

            // Rapid-fire heroic synthwave arpeggio chord (Minor 7th progression)
            val intervals = doubleArrayOf(1.0, 1.189, 1.498, 1.782, 2.0)
            for (interval in intervals) {
                val noteFreq = root * interval
                playSynthTone(
                    frequency = noteFreq,
                    durationMs = 120,
                    waveform = Waveform.SQUARE,
                    pitchBendEndFreq = noteFreq * 1.04, // Subtle cyberpunk pitch rise
                    envelopeDecay = 3.2
                )
                delay(60) // High-tempo overlapping chiptune feel
            }
        }
    }

    fun playGameOver() {
        if (!isSoundEnabled) return
        // Fat detuned analog power-down sweep from 300Hz down to a sub-bass 55Hz over 750ms
        playSynthTone(
            frequency = 300.0,
            durationMs = 750,
            waveform = Waveform.DETUNED_SAW,
            pitchBendEndFreq = 55.0,
            envelopeDecay = 1.5
        )
    }

    fun playNewHighScore() {
        if (!isSoundEnabled) return
        CoroutineScope(Dispatchers.Default).launch {
            // Elegant cyber melody arpeggio
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (note in notes) {
                playSynthTone(
                    frequency = note,
                    durationMs = 150,
                    waveform = Waveform.FM_PLUCK,
                    pitchBendEndFreq = note * 1.08,
                    envelopeDecay = 2.8
                )
                delay(85)
            }
        }
    }
}
