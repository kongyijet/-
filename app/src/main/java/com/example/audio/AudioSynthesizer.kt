package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioSynthesizer {
    private const val SAMPLE_RATE = 44100

    /**
     * Generates a seamless 1.5 - 2.5 second audio buffer for the chosen ringtone.
     */
    fun generateRingtoneBuffer(ringtoneId: String): ShortArray {
        return when (ringtoneId) {
            "classic_digital" -> generateClassicDigital()
            "morning_birds" -> generateMorningBirds()
            "gentle_chime" -> generateGentleChime()
            "energetic_beat" -> generateEnergeticBeat()
            "soft_piano" -> generateSoftPiano()
            "radar_alert" -> generateRadarAlert()
            else -> generateClassicDigital()
        }
    }

    private fun generateClassicDigital(): ShortArray {
        // Digital beep-beep-beep pattern (0.8s pattern looped)
        val durationSeconds = 1.2
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        val freq1 = 2048.0
        val freq2 = 2560.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 4 short beeps of 80ms with 40ms gaps, then rest
            val patternTime = t % 1.2
            val isBeep = (patternTime in 0.0..0.08) ||
                    (patternTime in 0.12..0.20) ||
                    (patternTime in 0.24..0.32) ||
                    (patternTime in 0.36..0.44)

            if (isBeep) {
                val wave = 0.7 * sin(2 * PI * freq1 * t) + 0.3 * sin(2 * PI * freq2 * t)
                buffer[i] = (wave * 28000).toInt().coerceIn(-32767, 32767).toShort()
            } else {
                buffer[i] = 0
            }
        }
        return buffer
    }

    private fun generateMorningBirds(): ShortArray {
        // Sweet arpeggiated melodic chirps with harmonic overtones
        val durationSeconds = 2.0
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        // Notes: E5 (659Hz), G#5 (830Hz), B5 (987Hz), E6 (1318Hz), G#6 (1661Hz)
        val noteFrequencies = doubleArrayOf(659.25, 830.61, 987.77, 1318.51, 1661.22, 1975.53)
        val noteDurations = doubleArrayOf(0.18, 0.18, 0.22, 0.25, 0.35, 0.45)
        val startTimes = doubleArrayOf(0.0, 0.22, 0.44, 0.70, 1.00, 1.40)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            for (n in noteFrequencies.indices) {
                val noteStart = startTimes[n]
                val noteDur = noteDurations[n]
                if (t >= noteStart && t < noteStart + noteDur) {
                    val localT = t - noteStart
                    val freq = noteFrequencies[n] + sin(localT * 35.0) * 20.0 // slight vibrato
                    val envelope = exp(-localT * 6.0) * sin(PI * (localT / noteDur).coerceIn(0.0, 1.0))
                    val wave = sin(2 * PI * freq * localT) + 0.4 * sin(4 * PI * freq * localT) + 0.2 * sin(6 * PI * freq * localT)
                    sample += wave * envelope * 0.7
                }
            }

            buffer[i] = (sample * 25000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateGentleChime(): ShortArray {
        // Marimba chord chime: C5 (523.25), E5 (659.25), G5 (783.99), B5 (987.77), C6 (1046.5)
        val durationSeconds = 2.2
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        val notes = doubleArrayOf(523.25, 659.25, 783.99, 987.77, 1046.50)
        val times = doubleArrayOf(0.0, 0.25, 0.50, 0.75, 1.05)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            for (n in notes.indices) {
                val noteStart = times[n]
                if (t >= noteStart) {
                    val localT = t - noteStart
                    val decay = exp(-localT * 3.5)
                    val freq = notes[n]
                    val wave = sin(2 * PI * freq * localT) +
                            0.35 * sin(2 * PI * freq * 2.76 * localT) * exp(-localT * 7.0) // metallic strike overtone
                    sample += wave * decay * 0.4
                }
            }

            buffer[i] = (sample * 27000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateEnergeticBeat(): ShortArray {
        // Fast energetic electro melodic sequence
        val durationSeconds = 1.6
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        // Melodic sequence: A4 (440), C5 (523), D5 (587), E5 (659), G5 (784), E5 (659), A5 (880)
        val scale = doubleArrayOf(440.0, 523.25, 587.33, 659.25, 783.99, 659.25, 880.0, 1046.5)
        val stepDuration = 0.18

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val step = ((t / stepDuration).toInt()) % scale.size
            val localT = t % stepDuration
            val freq = scale[step]

            // Punchy square-like sawtooth synth sound
            val decay = (1.0 - (localT / stepDuration)).coerceIn(0.0, 1.0)
            val base = sin(2 * PI * freq * localT) +
                    0.5 * sin(4 * PI * freq * localT) +
                    0.25 * sin(6 * PI * freq * localT)

            // Bass pulse
            val bass = if (localT < 0.1) sin(2 * PI * 110.0 * localT) * exp(-localT * 15.0) else 0.0

            val sample = (base * decay * 0.6) + (bass * 0.4)
            buffer[i] = (sample * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateSoftPiano(): ShortArray {
        // Warm relaxing piano chords: Fmaj7 -> G -> Am
        val durationSeconds = 2.4
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        val chord1 = doubleArrayOf(349.23, 440.00, 523.25, 659.25) // F A C E
        val chord2 = doubleArrayOf(440.00, 523.25, 659.25, 880.00) // A C E A

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            if (t < 1.2) {
                val localT = t
                val decay = exp(-localT * 2.2)
                for (f in chord1) {
                    val wave = sin(2 * PI * f * localT) + 0.3 * sin(4 * PI * f * localT)
                    sample += wave * decay * 0.25
                }
            } else {
                val localT = t - 1.2
                val decay = exp(-localT * 2.0)
                for (f in chord2) {
                    val wave = sin(2 * PI * f * localT) + 0.3 * sin(4 * PI * f * localT)
                    sample += wave * decay * 0.25
                }
            }

            buffer[i] = (sample * 28000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateRadarAlert(): ShortArray {
        // High-tech urgent sonar pulse
        val durationSeconds = 1.0
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val pingTime = t % 0.5
            var sample = 0.0

            if (pingTime < 0.25) {
                // Pitch sweep 1200Hz to 2800Hz
                val freq = 1200.0 + (pingTime / 0.25) * 1600.0
                val env = exp(-pingTime * 8.0) * (1.0 - (pingTime / 0.25))
                sample = sin(2 * PI * freq * pingTime) * env
            }

            buffer[i] = (sample * 29000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    fun createAudioTrack(buffer: ShortArray): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val bufferSizeBytes = buffer.size * 2

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        return audioTrack
    }
}
