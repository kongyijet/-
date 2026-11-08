package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.receiver.AlarmReceiver
import com.example.ui.alert.AlarmAlertActivity

object AlarmNotificationHelper {
    const val CHANNEL_ID_ALARM = "channel_alarm_ringing"
    const val CHANNEL_NAME_ALARM = "闹钟提醒"
    const val NOTIFICATION_ID_BASE = 10000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                CHANNEL_NAME_ALARM,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟响铃全屏提醒通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // Silence default notification sound because RingtonePlayer synthesizes the custom alarm tone
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        label: String,
        timeFormatted: String,
        snoozeMinutes: Int,
        ringtoneId: String,
        vibrate: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch fullscreen activity
        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmAlertActivity.EXTRA_LABEL, label)
            putExtra(AlarmAlertActivity.EXTRA_TIME, timeFormatted)
            putExtra(AlarmAlertActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmAlertActivity.EXTRA_RINGTONE_ID, ringtoneId)
            putExtra(AlarmAlertActivity.EXTRA_VIBRATE, vibrate)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action intent
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmAlertActivity.EXTRA_LABEL, label)
            putExtra(AlarmAlertActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmAlertActivity.EXTRA_RINGTONE_ID, ringtoneId)
            putExtra(AlarmAlertActivity.EXTRA_VIBRATE, vibrate)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ 闹钟响了：$label")
            .setContentText("时间：$timeFormatted - 点击查看全屏提醒")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "稍后提醒 (${snoozeMinutes}分钟)", snoozePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + alarmId.toInt(), notification)
    }

    fun dismissAlarmNotification(context: Context, alarmId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_BASE + alarmId.toInt())
    }
}
