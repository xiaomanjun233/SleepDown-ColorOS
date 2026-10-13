package com.xiaomanjun.sleepdownschedule

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeAlertRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeAlertRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.SleepDownPickerDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NativePickerLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun hostKeepsSelectionUntilNativeExitAndCreatesFreshWindowOnReopen() {
        val show = mutableStateOf(false)
        var request: NativeAlertRequest? = null
        var windows = 0
        var disposed = 0
        var requests = 0
        var completions = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalNativeAlertRenderer provides { current ->
                    SideEffect { request = current }
                    DisposableEffect(Unit) {
                        windows++
                        onDispose { disposed++ }
                    }
                    current.messageContent?.invoke()
                    true
                }) {
                    SleepDownPickerDialog(show.value, "选择日期", { requests++; show.value = false },
                        null, defaultConfig(), onDismissFinished = { completions++ },
                        titleAction = { Text("标题操作") }, bottomActions = { Text("完成操作") }) {
                        Text("保留的选择")
                    }
                }
            }
        }
        compose.runOnIdle { assertEquals(0, windows) }
        repeat(3) { cycle ->
            compose.runOnIdle { show.value = true }
            compose.onNodeWithText("选择日期").assertExists()
            compose.onNodeWithText("标题操作").assertExists()
            compose.onNodeWithText("完成操作").assertExists()
            compose.runOnIdle {
                assertEquals(cycle + 1, windows)
                assertTrue(request!!.deferDismissUntilHost)
                request!!.onDismissRequest()
            }
            compose.waitForIdle()
            compose.onNodeWithText("保留的选择").assertExists()
            compose.runOnIdle {
                assertFalse(request!!.visible)
                assertEquals(cycle, disposed)
                assertEquals(cycle, completions)
                request!!.onDismissFinished!!()
            }
            compose.onNodeWithText("保留的选择").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(cycle + 1, requests)
                assertEquals(cycle + 1, completions)
                assertEquals(cycle + 1, disposed)
            }
        }
    }
}
