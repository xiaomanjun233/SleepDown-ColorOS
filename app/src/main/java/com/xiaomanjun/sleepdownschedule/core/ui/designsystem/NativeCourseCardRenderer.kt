package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State

/** Vendor implementation is supplied by app assembly, keeping glass independent of features. */
internal typealias NativeCourseCardRequest = NativeMaterialSurfaceRequest

@Stable
internal class NativeCourseCardMode(
    private val enabledState: State<Boolean>,
    val onEnabledChange: (Boolean) -> Unit,
    private val tintState: State<Float>,
    private val blurScaleState: State<Float>,
    private val defaultBlurRadiusPx: Float,
    val onTintStrengthChange: (Float, Boolean) -> Unit,
    val onBlurRadiusScaleChange: (Float, Boolean) -> Unit,
    val onPreviewFinished: () -> Unit
) {
    val enabled: Boolean get() = enabledState.value
    val tintStrength: Float get() = tintState.value
    val blurRadiusScale: Float get() = blurScaleState.value
    val blurRadiusPx: Float get() = defaultBlurRadiusPx * blurRadiusScale
}

internal val LocalNativeCourseCardMode = compositionLocalOf<NativeCourseCardMode?> { null }
internal val LocalNativeCourseCardRenderer =
    compositionLocalOf<(@Composable (NativeCourseCardRequest, @Composable () -> Unit) -> Boolean)?> { null }
