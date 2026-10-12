package com.xiaomanjun.sleepdownschedule

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.xiaomanjun.sleepdownschedule.core.ui.settings.GlassMiuixSettingsTheme
import com.xiaomanjun.sleepdownschedule.feature.schedule.picker.QuickScheduleDraft
import com.xiaomanjun.sleepdownschedule.feature.schedule.picker.QuickScheduleSettingsSheets
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.glassBackdropProducer
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassLayerBackdrop
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Scaffold

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickScheduleSheetLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun reopeningSameScheduleRestoresContentAndGeometry() = reopen(withGlass = false)

    @Test fun reopeningGlassSheetRestoresContentAndGeometry() = reopen(withGlass = true)

    private fun reopen(withGlass: Boolean) {
        val draft = mutableStateOf<QuickScheduleDraft?>(null)
        var dismissals = 0
        var saves = 0
        compose.setContent {
            MaterialTheme {
                GlassMiuixSettingsTheme(defaultConfig()) {
                    val backdrop = rememberGlassLayerBackdrop(GlassBackdropDomain.Content, "sheet-test")
                    Scaffold(Modifier.fillMaxSize(),
                        underlayModifier = Modifier.fillMaxSize().glassBackdropProducer(backdrop).background(Color.DarkGray),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                        QuickScheduleSettingsSheets(draft.value, defaultConfig(), backdrop.takeIf { withGlass },
                            onDraftChange = { draft.value = it },
                            onDismiss = { draft.value = null },
                            onDismissFinished = { dismissals++ },
                            onSave = { _, done -> saves++; done() },
                            onDetailedSettings = { _, _, _ -> })
                    }
                }
            }
        }
        var originalTitle: Rect? = null
        var originalDetail: Rect? = null
        repeat(3) { cycle ->
            compose.runOnIdle { draft.value = QuickScheduleDraft(1, 20, 1, true, false, "2026-09-01") }
            compose.waitForIdle()
            compose.onNodeWithText("总周数").assertExists()
            val title = compose.onNodeWithText("课表设置").fetchSemanticsNode().boundsInRoot
            val detail = compose.onNodeWithText("详细设置").fetchSemanticsNode().boundsInRoot
            if (cycle == 0) { originalTitle = title; originalDetail = detail }
            assertEquals("Reopen $cycle title", originalTitle, title)
            assertEquals("Reopen $cycle bottom action", originalDetail, detail)
            compose.mainClock.autoAdvance = false
            compose.onNodeWithText("完成").performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            // Closing must retain the same form until its exit animation has completed.
            compose.onNodeWithText("20").assertExists()
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.onNodeWithText("课表设置").assertDoesNotExist()
            assertEquals(cycle + 1, saves)
            assertEquals(cycle + 1, dismissals)
        }
    }
}
