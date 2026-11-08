package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hour: Int, // 0 - 23
    val minute: Int, // 0 - 59
    val label: String = "闹钟",
    val isEnabled: Boolean = true,
    val repeatDaysMask: Int = 0, // 0 = 仅一次 (Once)
    val ringtoneId: String = "classic_digital",
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MASK_MON = 1
        const val MASK_TUE = 2
        const val MASK_WED = 4
        const val MASK_THU = 8
        const val MASK_FRI = 16
        const val MASK_SAT = 32
        const val MASK_SUN = 64

        const val MASK_WEEKDAYS = MASK_MON or MASK_TUE or MASK_WED or MASK_THU or MASK_FRI
        const val MASK_WEEKENDS = MASK_SAT or MASK_SUN
        const val MASK_EVERY_DAY = MASK_WEEKDAYS or MASK_WEEKENDS
        const val MASK_ONCE = 0
    }

    val isRepeating: Boolean
        get() = repeatDaysMask != 0

    fun isDaySelected(dayMask: Int): Boolean {
        return (repeatDaysMask and dayMask) != 0
    }

    fun getRepeatDescription(): String {
        return when (repeatDaysMask) {
            MASK_ONCE -> "仅一次"
            MASK_EVERY_DAY -> "每天"
            MASK_WEEKDAYS -> "工作日"
            MASK_WEEKENDS -> "周末"
            else -> {
                val days = mutableListOf<String>()
                if (isDaySelected(MASK_MON)) days.add("一")
                if (isDaySelected(MASK_TUE)) days.add("二")
                if (isDaySelected(MASK_WED)) days.add("三")
                if (isDaySelected(MASK_THU)) days.add("四")
                if (isDaySelected(MASK_FRI)) days.add("五")
                if (isDaySelected(MASK_SAT)) days.add("六")
                if (isDaySelected(MASK_SUN)) days.add("日")
                if (days.isEmpty()) "仅一次" else "周" + days.joinToString("、")
            }
        }
    }

    /**
     * Calculates the exact next trigger time in milliseconds.
     */
    fun calculateNextTriggerTime(fromMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeatDaysMask == MASK_ONCE) {
            // One-off alarm: if target is before now, it's scheduled for tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Repeating alarm: find the closest scheduled day of week
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val calDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            val dayMask = when (calDayOfWeek) {
                Calendar.MONDAY -> MASK_MON
                Calendar.TUESDAY -> MASK_TUE
                Calendar.WEDNESDAY -> MASK_WED
                Calendar.THURSDAY -> MASK_THU
                Calendar.FRIDAY -> MASK_FRI
                Calendar.SATURDAY -> MASK_SAT
                Calendar.SUNDAY -> MASK_SUN
                else -> 0
            }

            if (isDaySelected(dayMask)) {
                if (dayOffset == 0) {
                    if (candidate.timeInMillis > now.timeInMillis) {
                        return candidate.timeInMillis
                    }
                } else {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    /**
     * Formats 24h or 12h display
     */
    fun formatTime(is24Hour: Boolean = true): String {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val period = if (hour < 12) "AM" else "PM"
            String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, period)
        }
    }

    /**
     * Returns Chinese period (上午 / 下午 / 凌晨 / 晚上)
     */
    fun getTimePeriodDescription(): String {
        return when (hour) {
            in 0..5 -> "凌晨"
            in 6..11 -> "上午"
            12 -> "中午"
            in 13..17 -> "下午"
            else -> "晚上"
        }
    }
}
