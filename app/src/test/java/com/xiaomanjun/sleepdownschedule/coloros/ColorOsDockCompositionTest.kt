package com.xiaomanjun.sleepdownschedule.coloros

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.DockAlignment
import com.xiaomanjun.sleepdownschedule.core.performance.StartupPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLog

/** Exercises the actual Dock composition boundary; vendor View drawing still needs a device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ColorOsDockCompositionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun changingBackgroundCapturedByFallbackDoesNotCommitDockHostAgain() {
        val backgroundRevision = mutableIntStateOf(0)
        var renderedFallbackRevision = -1
        val select: (Int) -> Unit = {}
        val home: () -> Unit = {}
        val menu: (Rect, GraphicsLayer) -> Unit = { _, _ -> }
        ShadowLog.clear()
        compose.setContent {
            val revision = backgroundRevision.intValue
            val fallback = rememberUpdatedState<@Composable () -> Unit> {
                Box(Modifier.size(64.dp))
                SideEffect { renderedFallbackRevision = revision }
            }
            NativeHomeDockChrome(true, true, 1, false, DockAlignment.CENTER, false,
                select, home, menu, fallback, StartupPhase.FullQuality)
        }
        compose.waitForIdle()
        fun commits() = ShadowLog.getLogsForTag("SleepDownCOUI").count { it.msg.startsWith("Native dock commit:") }
        val initial = commits()
        assertTrue(initial > 0)
        for (revision in 1..12) {
            compose.runOnIdle { backgroundRevision.intValue = revision }
            compose.waitForIdle()
        }
        assertEquals("The fallback must still receive fresh state", 12, renderedFallbackRevision)
        assertEquals("Background-only changes must not commit the native Dock host", initial, commits())
    }
}
