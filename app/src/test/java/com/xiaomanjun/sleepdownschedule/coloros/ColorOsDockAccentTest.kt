package com.xiaomanjun.sleepdownschedule.coloros

import android.app.Application
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ColorOsDockAccentTest {
    @Test fun wallpaperAccentsStayReadableOnBothBackgroundsAndUnderWhitePlus() {
        for (seed in listOf(0xff2563eb.toInt(), 0xffb91c1c.toInt(), 0xff16a34a.toInt(), 0xffeab308.toInt())) {
            val darkBackground = 0xff151820.toInt()
            val lightBackground = 0xffeeeeee.toInt()
            val lightAccent = colorOsContrastingAccent(seed, darkBackground)
            val darkAccent = colorOsContrastingAccent(seed, lightBackground)
            assertTrue(ColorUtils.calculateLuminance(lightAccent) > ColorUtils.calculateLuminance(darkAccent))
            assertTrue(ColorUtils.calculateContrast(lightAccent, darkBackground) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(darkAccent, lightBackground) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(Color.WHITE, colorOsAccentForWhiteIcon(lightAccent)) >= 3.0)
        }
    }
}
