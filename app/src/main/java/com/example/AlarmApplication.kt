package com.example

import android.app.Application
import com.example.data.local.AlarmDatabase
import com.example.data.repository.AlarmRepository
import com.example.notification.AlarmNotificationHelper
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmApplication : Application() {

    val database by lazy { AlarmDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), this) }

    override fun onCreate() {
        super.onCreate()
        AlarmNotificationHelper.createNotificationChannels(this)

        // Reschedule active alarms on app startup to make sure AlarmManager has all alarms registered
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeAlarms = database.alarmDao().getActiveAlarmsSync()
                for (alarm in activeAlarms) {
                    AlarmScheduler.schedule(this@AlarmApplication, alarm)
                }
            } catch (e: Exception) {
                // Ignore initial empty db
            }
        }
    }
}
