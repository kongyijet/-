package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object VibratorHelper {
    private const val TAG = "VibratorHelper"
    private var vibrator: Vibrator? = null

    private fun getVibrator(context: Context): Vibrator? {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
        return vibrator
    }

    fun startVibration(context: Context) {
        try {
            val vib = getVibrator(context) ?: return
            val pattern = longArrayOf(0, 500, 300, 500, 300, 700)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // loop from index 0
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    fun stopVibration(context: Context) {
        try {
            val vib = getVibrator(context)
            vib?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel vibration", e)
        }
    }
}
