package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.audio.RingtonePlayer
import com.example.audio.VibratorHelper
import com.example.data.local.AlarmDatabase
import com.example.data.model.AlarmEntity
import com.example.notification.AlarmNotificationHelper
import com.example.service.AlarmScheduler
import com.example.ui.alert.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.aistudio.alarmclock.ACTION_TRIGGER_ALARM"
        const val ACTION_DISMISS_ALARM = "com.aistudio.alarmclock.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.aistudio.alarmclock.ACTION_SNOOZE_ALARM"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_RINGTONE_ID = "extra_ringtone_id"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_REPEAT_MASK = "extra_repeat_mask"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "闹钟"
        val ringtoneId = intent.getStringExtra(EXTRA_RINGTONE_ID) ?: "classic_digital"
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
        val repeatMask = intent.getIntExtra(EXTRA_REPEAT_MASK, 0)

        Log.d(TAG, "Received action: $action for alarm ID: $alarmId")

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                // Acquire temporary WakeLock to guarantee screen turns on
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "AlarmClock:AlarmTriggerWakeLock"
                )
                wakeLock?.acquire(30_000L) // 30s timeout

                val cal = Calendar.getInstance()
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

                // 1. Show Heads-up / Full-screen notification
                AlarmNotificationHelper.showAlarmNotification(
                    context = context,
                    alarmId = alarmId,
                    label = label,
                    timeFormatted = timeFormatted,
                    snoozeMinutes = snoozeMinutes,
                    ringtoneId = ringtoneId,
                    vibrate = vibrate
                )

                // 2. Launch AlarmAlertActivity fullscreen
                val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmAlertActivity.EXTRA_LABEL, label)
                    putExtra(AlarmAlertActivity.EXTRA_TIME, timeFormatted)
                    putExtra(AlarmAlertActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                    putExtra(AlarmAlertActivity.EXTRA_RINGTONE_ID, ringtoneId)
                    putExtra(AlarmAlertActivity.EXTRA_VIBRATE, vibrate)
                }
                context.startActivity(alertIntent)

                // 3. Handle repeat vs one-off
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AlarmDatabase.getDatabase(context)
                    if (alarmId > 0) {
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            if (alarm.repeatDaysMask == AlarmEntity.MASK_ONCE) {
                                // One-off alarm: disable it
                                db.alarmDao().setAlarmEnabled(alarmId, false)
                            } else {
                                // Repeating alarm: reschedule for next occurrence
                                AlarmScheduler.schedule(context, alarm)
                            }
                        }
                    }
                }
            }

            ACTION_DISMISS_ALARM -> {
                RingtonePlayer.stop()
                VibratorHelper.stopVibration(context)
                AlarmNotificationHelper.dismissAlarmNotification(context, alarmId)
            }

            ACTION_SNOOZE_ALARM -> {
                RingtonePlayer.stop()
                VibratorHelper.stopVibration(context)
                AlarmNotificationHelper.dismissAlarmNotification(context, alarmId)
                AlarmScheduler.scheduleSnooze(context, alarmId, label, snoozeMinutes, ringtoneId, vibrate)
            }
        }
    }
}
