package com.example.audio

import android.content.Context
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.util.Log

object RingtonePlayer {
    private const val TAG = "RingtonePlayer"
    private var currentAudioTrack: AudioTrack? = null
    private var previewHandler = Handler(Looper.getMainLooper())
    private var isLooping = false
    private var isPlaying = false

    @Synchronized
    fun playRingtone(context: Context, ringtoneId: String, loop: Boolean = true) {
        stop()
        try {
            val buffer = AudioSynthesizer.generateRingtoneBuffer(ringtoneId)
            val track = AudioSynthesizer.createAudioTrack(buffer)
            currentAudioTrack = track

            if (loop) {
                track.setLoopPoints(0, buffer.size, -1) // infinite loop
            } else {
                track.setLoopPoints(0, buffer.size, 1)
            }

            track.play()
            isLooping = loop
            isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play synthesized ringtone, falling back", e)
            playSystemFallback(context)
        }
    }

    @Synchronized
    fun previewRingtone(context: Context, ringtoneId: String, durationMs: Long = 3000L, onComplete: (() -> Unit)? = null) {
        stop()
        try {
            val buffer = AudioSynthesizer.generateRingtoneBuffer(ringtoneId)
            val track = AudioSynthesizer.createAudioTrack(buffer)
            currentAudioTrack = track
            track.setLoopPoints(0, buffer.size, -1)
            track.play()
            isPlaying = true

            previewHandler.removeCallbacksAndMessages(null)
            previewHandler.postDelayed({
                stop()
                onComplete?.invoke()
            }, durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Preview failed", e)
            stop()
            onComplete?.invoke()
        }
    }

    @Synchronized
    fun stop() {
        previewHandler.removeCallbacksAndMessages(null)
        try {
            currentAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio track", e)
        } finally {
            currentAudioTrack = null
            isPlaying = false
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying

    private fun playSystemFallback(context: Context) {
        try {
            val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "System fallback failed", e)
        }
    }
}
