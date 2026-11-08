package com.example.data.repository

import android.content.Context
import com.example.data.local.AlarmDao
import com.example.data.model.AlarmEntity
import com.example.service.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val context: Context
) {
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Long): AlarmEntity? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun saveAlarm(alarm: AlarmEntity): Long {
        val id = if (alarm.id == 0L) {
            alarmDao.insertAlarm(alarm)
        } else {
            alarmDao.updateAlarm(alarm)
            alarm.id
        }

        val updatedAlarm = alarm.copy(id = id)
        if (updatedAlarm.isEnabled) {
            AlarmScheduler.schedule(context, updatedAlarm)
        } else {
            AlarmScheduler.cancel(context, updatedAlarm.id)
        }
        return id
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        val updated = alarm.copy(isEnabled = isEnabled)
        alarmDao.updateAlarm(updated)
        if (isEnabled) {
            AlarmScheduler.schedule(context, updated)
        } else {
            AlarmScheduler.cancel(context, alarm.id)
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        AlarmScheduler.cancel(context, alarm.id)
        alarmDao.deleteAlarm(alarm)
    }
}
