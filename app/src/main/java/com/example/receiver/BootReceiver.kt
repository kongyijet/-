package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AlarmDatabase
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver triggered with action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == action ||
            Intent.ACTION_TIME_CHANGED == action ||
            Intent.ACTION_TIMEZONE_CHANGED == action
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val activeAlarms = db.alarmDao().getActiveAlarmsSync()
                    Log.d(TAG, "Rescheduling ${activeAlarms.size} active alarms after reboot/time change")
                    for (alarm in activeAlarms) {
                        AlarmScheduler.schedule(context, alarm)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
