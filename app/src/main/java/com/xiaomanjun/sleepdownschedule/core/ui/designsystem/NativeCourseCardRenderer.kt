package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Vendor implementation is supplied by app assembly, keeping glass independent of features. */
internal typealias NativeCourseCardRequest = NativeMaterialSurfaceRequest

internal data class NativeCourseCardMode(
    val enabled: Boolean,
    val onEnabledChange: (Boolean) -> Unit,
    val tintStrength: Float,
    val blurRadiusPx: Float,
    val onTintStrengthChange: (Float, Boolean) -> Unit,
    val onPreviewFinished: () -> Unit
)

internal val LocalNativeCourseCardMode = compositionLocalOf<NativeCourseCardMode?> { null }
internal val LocalNativeCourseCardRenderer =
    compositionLocalOf<(@Composable (NativeCourseCardRequest, @Composable () -> Unit) -> Boolean)?> { null }
