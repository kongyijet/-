package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmApplication
import com.example.audio.RingtonePlayer
import com.example.data.model.AlarmEntity
import com.example.ui.alert.AlarmAlertActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ClockDisplayState(
    val timeFormatted: String = "00:00:00",
    val hourPart: String = "00",
    val minutePart: String = "00",
    val secondPart: String = "00",
    val amPmPeriod: String = "AM",
    val dateFormatted: String = "",
    val dayOfWeek: String = ""
)

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AlarmApplication).repository

    val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _is24HourFormat = MutableStateFlow(true)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _clockState = MutableStateFlow(ClockDisplayState())
    val clockState: StateFlow<ClockDisplayState> = _clockState.asStateFlow()

    // Add / Edit sheet state
    private val _isEditSheetVisible = MutableStateFlow(false)
    val isEditSheetVisible: StateFlow<Boolean> = _isEditSheetVisible.asStateFlow()

    private val _currentEditingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val currentEditingAlarm: StateFlow<AlarmEntity?> = _currentEditingAlarm.asStateFlow()

    // Ringtone preview state in picker
    private val _previewingRingtoneId = MutableStateFlow<String?>(null)
    val previewingRingtoneId: StateFlow<String?> = _previewingRingtoneId.asStateFlow()

    init {
        // Start real-time clock ticker updating each second
        viewModelScope.launch {
            while (true) {
                updateClock()
                delay(1000L - (System.currentTimeMillis() % 1000L))
            }
        }
    }

    private fun updateClock() {
        val now = Calendar.getInstance()
        val is24H = _is24HourFormat.value

        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val second = now.get(Calendar.SECOND)

        val hourDisplay = if (is24H) {
            String.format(Locale.getDefault(), "%02d", hour)
        } else {
            val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            String.format(Locale.getDefault(), "%02d", h12)
        }
        val minDisplay = String.format(Locale.getDefault(), "%02d", minute)
        val secDisplay = String.format(Locale.getDefault(), "%02d", second)
        val amPm = if (hour < 12) "上午 AM" else "下午 PM"

        val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.CHINESE)

        _clockState.value = ClockDisplayState(
            timeFormatted = "$hourDisplay:$minDisplay:$secDisplay",
            hourPart = hourDisplay,
            minutePart = minDisplay,
            secondPart = secDisplay,
            amPmPeriod = amPm,
            dateFormatted = dateFormat.format(now.time),
            dayOfWeek = dayOfWeekFormat.format(now.time)
        )
    }

    fun toggleTimeFormat() {
        _is24HourFormat.value = !_is24HourFormat.value
        updateClock()
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, isEnabled)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun openAddAlarm() {
        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, 5)
        _currentEditingAlarm.value = AlarmEntity(
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = now.get(Calendar.MINUTE),
            label = "起床",
            isEnabled = true,
            repeatDaysMask = AlarmEntity.MASK_ONCE,
            ringtoneId = "classic_digital",
            vibrate = true,
            snoozeMinutes = 5
        )
        _isEditSheetVisible.value = true
    }

    fun openEditAlarm(alarm: AlarmEntity) {
        _currentEditingAlarm.value = alarm
        _isEditSheetVisible.value = true
    }

    fun dismissEditSheet() {
        stopRingtonePreview()
        _isEditSheetVisible.value = false
        _currentEditingAlarm.value = null
    }

    fun saveAlarm(alarm: AlarmEntity) {
        stopRingtonePreview()
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            dismissEditSheet()
        }
    }

    fun previewRingtone(ringtoneId: String) {
        if (_previewingRingtoneId.value == ringtoneId) {
            stopRingtonePreview()
        } else {
            _previewingRingtoneId.value = ringtoneId
            RingtonePlayer.previewRingtone(
                context = getApplication(),
                ringtoneId = ringtoneId,
                durationMs = 4000L,
                onComplete = {
                    _previewingRingtoneId.value = null
                }
            )
        }
    }

    fun stopRingtonePreview() {
        RingtonePlayer.stop()
        _previewingRingtoneId.value = null
    }

    /**
     * Requirement 16: Immediate alarm test button to test ringing & UI directly
     */
    fun testAlarmImmediately(label: String = "测试闹钟", ringtoneId: String = "classic_digital", snoozeMinutes: Int = 5) {
        val context = getApplication<Application>()
        val cal = Calendar.getInstance()
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

        val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmAlertActivity.EXTRA_ALARM_ID, -99L)
            putExtra(AlarmAlertActivity.EXTRA_LABEL, label)
            putExtra(AlarmAlertActivity.EXTRA_TIME, timeFormatted)
            putExtra(AlarmAlertActivity.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmAlertActivity.EXTRA_RINGTONE_ID, ringtoneId)
            putExtra(AlarmAlertActivity.EXTRA_VIBRATE, true)
            putExtra(AlarmAlertActivity.EXTRA_IS_TEST, true)
        }
        context.startActivity(alertIntent)
    }

    /**
     * Calculates time remaining until the next active alarm
     */
    fun getNextUpcomingAlarmText(alarmList: List<AlarmEntity>): String {
        val activeAlarms = alarmList.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            return "暂无开启的闹钟"
        }

        val now = System.currentTimeMillis()
        var nearestTime = Long.MAX_VALUE
        var nearestAlarm: AlarmEntity? = null

        for (alarm in activeAlarms) {
            val trigger = alarm.calculateNextTriggerTime(now)
            if (trigger in now until nearestTime) {
                nearestTime = trigger
                nearestAlarm = alarm
            }
        }

        if (nearestAlarm == null || nearestTime == Long.MAX_VALUE) {
            return "暂无开启的闹钟"
        }

        val diffMillis = nearestTime - now
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60

        val timeStr = nearestAlarm.formatTime(_is24HourFormat.value)
        val diffStr = when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分钟"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分钟"
            else -> "少于1分钟"
        }

        return "下一个闹钟：$timeStr (${nearestAlarm.label}) · 还有 $diffStr"
    }

    override fun onCleared() {
        super.onCleared()
        RingtonePlayer.stop()
    }
}
