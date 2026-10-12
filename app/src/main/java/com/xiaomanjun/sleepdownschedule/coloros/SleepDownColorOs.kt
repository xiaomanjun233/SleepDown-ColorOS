package com.xiaomanjun.sleepdownschedule.coloros

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeAlertRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCapsuleButtonRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeProgressiveBlur

/** SleepDown's ColorOS framework entry. Each capability returns false when unavailable. */
@Composable
internal fun ProvideSleepDownColorOs(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNativeAlertRenderer provides { request -> ColorOsAlertDialog(request) },
        LocalNativeCapsuleButtonRenderer provides { request, body -> ColorOsCapsuleButton(request, body) },
        LocalNativeCourseCardMode provides rememberColorOsCourseCardMode(),
        LocalNativeCourseCardRenderer provides { request, body -> ColorOsCourseCard(request, body) },
        LocalNativeProgressiveBlur provides rememberColorOsProgressiveBlur(),
        content = content
    )
}
