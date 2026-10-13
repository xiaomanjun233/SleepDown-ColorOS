package com.xiaomanjun.sleepdownschedule.coloros

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativePopupRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativePopupRequest
import com.xiaomanjun.sleepdownschedule.core.ui.settings.GlassMiuixSettingsTheme
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidDropdownPreference
import com.xiaomanjun.sleepdownschedule.defaultConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks the preference/window handoff; vendor animation and drawing require a device. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NativePopupPreferenceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun nativeSelectionAndDismissAllowTheSamePreferenceToReopen() {
        val selected = mutableIntStateOf(0)
        val selections = mutableListOf<Int>()
        val expandedEvents = mutableListOf<Boolean>()
        lateinit var window: NativePopupRequest
        compose.setContent {
            MaterialTheme {
                GlassMiuixSettingsTheme(defaultConfig()) {
                    CompositionLocalProvider(LocalNativePopupRenderer provides { request ->
                        SideEffect { window = request }
                        true
                    }) {
                        SleepDownLiquidDropdownPreference(
                            title = "选择外观", items = listOf("浅色", "深色"),
                            selectedIndex = selected.intValue, backdrop = null, config = defaultConfig(),
                            onExpandedChange = { expandedEvents += it },
                            onSelectedIndexChange = { selected.intValue = it; selections += it }
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        assertFalse(window.show)
        for (target in listOf(1, 0)) {
            compose.onNodeWithText("选择外观").performClick()
            compose.runOnIdle {
                assertTrue(window.show)
                assertTrue(window.anchorBounds.width > 0f && window.anchorBounds.height > 0f)
                assertTrue(window.items[selected.intValue].selected)
                // COUI finishes its exit before delivering the selected business action.
                window.onDismissRequest()
                window.items[target].onClick()
            }
            compose.waitForIdle()
            assertFalse(window.show)
            assertEquals(target, selected.intValue)
        }
        assertEquals(listOf(1, 0), selections)
        assertEquals(listOf(true, false, true, false), expandedEvents)
    }
}
