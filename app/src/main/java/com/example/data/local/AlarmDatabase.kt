package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_clock_database"
                )
                    .addCallback(AlarmDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AlarmDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialAlarms(database.alarmDao())
                    }
                }
            }

            suspend fun populateInitialAlarms(alarmDao: AlarmDao) {
                // Preset example alarms as mentioned by user: 起床, 上学, 写作业, 游戏时间结束
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 7,
                        minute = 0,
                        label = "起床",
                        isEnabled = true,
                        repeatDaysMask = AlarmEntity.MASK_WEEKDAYS,
                        ringtoneId = "morning_birds",
                        vibrate = true,
                        snoozeMinutes = 5
                    )
                )
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 8,
                        minute = 0,
                        label = "上学",
                        isEnabled = true,
                        repeatDaysMask = AlarmEntity.MASK_WEEKDAYS,
                        ringtoneId = "energetic_beat",
                        vibrate = true,
                        snoozeMinutes = 5
                    )
                )
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 17,
                        minute = 30,
                        label = "写作业",
                        isEnabled = false,
                        repeatDaysMask = AlarmEntity.MASK_WEEKDAYS,
                        ringtoneId = "gentle_chime",
                        vibrate = true,
                        snoozeMinutes = 10
                    )
                )
                alarmDao.insertAlarm(
                    AlarmEntity(
                        hour = 21,
                        minute = 0,
                        label = "游戏时间结束",
                        isEnabled = true,
                        repeatDaysMask = AlarmEntity.MASK_EVERY_DAY,
                        ringtoneId = "classic_digital",
                        vibrate = true,
                        snoozeMinutes = 5
                    )
                )
            }
        }
    }
}
