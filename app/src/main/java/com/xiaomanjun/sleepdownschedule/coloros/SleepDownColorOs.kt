package com.xiaomanjun.sleepdownschedule.coloros

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeAlertRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCapsuleButtonRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMaterialSurfaceRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeProgressiveBlur
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeSliderRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeSwitchRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeSegmentRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMenuRowRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMenuSurfaceRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativePopupRenderer

/** SleepDown's ColorOS framework entry. Each capability returns false when unavailable. */
@Composable
internal fun ProvideSleepDownColorOs(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNativeAlertRenderer provides { request -> ColorOsAlertDialog(request) },
        LocalNativeCapsuleButtonRenderer provides { request, body -> ColorOsCapsuleButton(request, body) },
        LocalNativeCourseCardMode provides rememberColorOsCourseCardMode(),
        LocalNativeCourseCardRenderer provides { request, body -> ColorOsCourseCard(request, body) },
        LocalNativeMaterialSurfaceRenderer provides { request, body -> ColorOsMaterialSurface(request, body) },
        LocalNativeProgressiveBlur provides rememberColorOsProgressiveBlur(),
        LocalNativeSliderRenderer provides { ColorOsSlider(it) },
        LocalNativeSwitchRenderer provides { ColorOsSwitch(it) },
        LocalNativeSegmentRenderer provides { ColorOsSegments(it) },
        LocalNativeMenuRowRenderer provides { ColorOsMenuRow(it) },
        LocalNativeMenuSurfaceRenderer provides { request, body -> ColorOsMenuSurface(request, body) },
        LocalNativePopupRenderer provides { ColorOsPopup(it) },
        content = content
    )
}
