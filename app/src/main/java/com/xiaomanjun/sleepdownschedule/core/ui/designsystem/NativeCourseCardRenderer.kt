package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Vendor implementation is supplied by app assembly, keeping glass independent of features. */
internal data class NativeCourseCardRequest(
    val modifier: Modifier, val shape: Shape, val dark: Boolean, val color: Color,
    val tintAlpha: Float, val blurRadiusPx: Float, val renderSurface: Boolean,
    val materialVisible: Boolean, val onClick: (() -> Unit)?,
    val onDrawTint: (DrawScope.() -> Unit)? = null
)

internal data class NativeCourseCardMode(
    val enabled: Boolean,
    val onEnabledChange: (Boolean) -> Unit,
    val tintStrength: Float,
    val onTintStrengthChange: (Float, Boolean) -> Unit,
    val onPreviewFinished: () -> Unit
)

internal val LocalNativeCourseCardMode = compositionLocalOf<NativeCourseCardMode?> { null }
internal val LocalNativeCourseCardRenderer =
    compositionLocalOf<(@Composable (NativeCourseCardRequest, @Composable () -> Unit) -> Boolean)?> { null }
