package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope

/** A passive material background. Geometry stays with its owner; content stays in Compose. */
internal data class NativeMaterialSurfaceRequest(
    val modifier: Modifier, val shape: Shape, val dark: Boolean,
    val renderSurface: Boolean = true, val materialVisible: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val onDrawTint: (DrawScope.() -> Unit)? = null,
    val shapeProvider: (() -> Shape)? = null,
    val surfaceAlpha: () -> Float = { 1f },
    val edgeEffects: Boolean = true,
    val accent: Int? = null
)

internal val LocalNativeMaterialSurfaceRenderer =
    compositionLocalOf<(@Composable (NativeMaterialSurfaceRequest, @Composable () -> Unit) -> Boolean)?> { null }
