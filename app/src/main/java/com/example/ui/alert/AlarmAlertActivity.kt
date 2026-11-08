package com.example.ui.alert

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RingtoneCatalog
import com.example.audio.RingtonePlayer
import com.example.audio.VibratorHelper
import com.example.notification.AlarmNotificationHelper
import com.example.receiver.AlarmReceiver
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmAlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_RINGTONE_ID = "extra_ringtone_id"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_IS_TEST = "extra_is_test"
    }

    private var alarmId: Long = -1L
    private var label: String = "闹钟"
    private var snoozeMinutes: Int = 5
    private var ringtoneId: String = "classic_digital"
    private var vibrate: Boolean = true
    private var isTest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn on screen & show over lockscreen
        turnScreenOnAndKeyguard()

        alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        label = intent.getStringExtra(EXTRA_LABEL) ?: "闹钟"
        snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
        ringtoneId = intent.getStringExtra(EXTRA_RINGTONE_ID) ?: "classic_digital"
        vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        // Start ringing sound and vibration
        RingtonePlayer.playRingtone(this, ringtoneId, loop = true)
        if (vibrate) {
            VibratorHelper.startVibration(this)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) { // Always striking dark mode for alarms
                AlarmAlertScreen(
                    alarmLabel = label,
                    snoozeMinutes = snoozeMinutes,
                    ringtoneName = RingtoneCatalog.getRingtoneById(ringtoneId).name,
                    isTest = isTest,
                    onStop = { stopAndDismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun stopAndDismissAlarm() {
        RingtonePlayer.stop()
        VibratorHelper.stopVibration(this)
        AlarmNotificationHelper.dismissAlarmNotification(this, alarmId)
        finish()
    }

    private fun snoozeAlarm() {
        RingtonePlayer.stop()
        VibratorHelper.stopVibration(this)
        AlarmNotificationHelper.dismissAlarmNotification(this, alarmId)

        if (!isTest && alarmId > 0) {
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_RINGTONE_ID, ringtoneId)
                putExtra(EXTRA_VIBRATE, vibrate)
            }
            sendBroadcast(snoozeIntent)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        RingtonePlayer.stop()
        VibratorHelper.stopVibration(this)
    }
}

@Composable
fun AlarmAlertScreen(
    alarmLabel: String,
    snoozeMinutes: Int,
    ringtoneName: String,
    isTest: Boolean,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTimeString by remember {
        mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
    }
    var currentDateString by remember {
        mutableStateOf(SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            currentDateString = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date())
            delay(1000L)
        }
    }

    // Pulsing animation
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        pulseScale.animateTo(
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0A0E1A)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Title & Test badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTest) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "🔔 测试闹钟模式",
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "⏰ 闹钟响了！",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentDateString,
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp
                )
            }

            // Center Section: Clock, Ringing Bell animation & Label Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing glowing bell icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale.value)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                    )
                    // Mid ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.35f))
                    )
                    // Inner icon circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Ringing",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time Display
                Text(
                    text = currentTimeString,
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Label card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = alarmLabel,
                            color = Color(0xFF38BDF8),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "铃声：$ringtoneName",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Bottom Section: Large Tactile Buttons (Stop and Snooze)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // STOP BUTTON (Primary / Prominent)
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("stop_alarm_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "停止闹钟",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // SNOOZE BUTTON
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("snooze_alarm_button"),
                    shape = RoundedCornerShape(24.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = "Snooze",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "稍后提醒 (${snoozeMinutes}分钟)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}
