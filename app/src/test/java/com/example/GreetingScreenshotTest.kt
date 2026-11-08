package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.ClockHeader
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClockDisplayState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ClockHeader(
          clockState = ClockDisplayState(
            timeFormatted = "07:30:00",
            hourPart = "07",
            minutePart = "30",
            secondPart = "00",
            amPmPeriod = "上午 AM",
            dateFormatted = "8月25日",
            dayOfWeek = "星期二"
          ),
          is24Hour = true,
          nextAlarmText = "下一个闹钟：07:30 (起床) · 还有 10分钟",
          onToggleTimeFormat = {},
          onTestAlarm = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

