package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.AlarmEntity
import com.example.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun schedule(context: Context, alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancel(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = alarm.calculateNextTriggerTime()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_MINUTE, alarm.minute)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_ID, alarm.ringtoneId)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_REPEAT_MASK, alarm.repeatDaysMask)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm ID ${alarm.id} for $triggerTime (${alarm.label})")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback scheduling", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun scheduleSnooze(
        context: Context,
        alarmId: Long,
        label: String,
        snoozeMinutes: Int,
        ringtoneId: String,
        vibrate: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_LABEL, "$label (稍后提醒)")
            putExtra(AlarmReceiver.EXTRA_RINGTONE_ID, ringtoneId)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmReceiver.EXTRA_REPEAT_MASK, 0) // one-off snooze
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 99999).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Snoozed alarm ID $alarmId for $snoozeMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule snooze", e)
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Also cancel any snooze pending intent
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 99999).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }
}
