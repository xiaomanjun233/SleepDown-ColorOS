package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Menu data and body rendering only. The caller keeps positioning, transitions and navigation. */
internal data class NativeMenuItem(
    val title: String,
    val iconRes: Int? = null,
    val description: String? = null,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val checkable: Boolean = false,
    val foreground: Int? = null,
    val iconTint: Int? = null,
    val children: List<NativeMenuItem> = emptyList(),
    val onClick: () -> Unit = {}
)

internal data class NativeMenuRowRequest(
    val item: NativeMenuItem,
    val dark: Boolean,
    val modifier: Modifier,
    val pressed: Boolean = false,
    val first: Boolean = false,
    val last: Boolean = false,
    val indicatorRotation: () -> Float = { 0f }
)

internal data class NativeMenuSurfaceRequest(
    val surface: NativeMaterialSurfaceRequest,
    val neutral: Boolean = false,
    val touchSource: NativeMenuTouchSource? = null
)

/** A popup draws its surface and rows as siblings, so its common host forwards touch here. */
internal class NativeMenuTouchSource {
    var onTouch: ((MotionEvent) -> Unit)? = null
    fun dispatch(event: MotionEvent) { onTouch?.invoke(event) }
}

internal val LocalNativeMenuRowRenderer =
    compositionLocalOf<(@Composable (NativeMenuRowRequest) -> Boolean)?> { null }
internal val LocalNativeMenuSurfaceRenderer =
    compositionLocalOf<(@Composable (NativeMenuSurfaceRequest, @Composable () -> Unit) -> Boolean)?> { null }

internal data class NativePopupRequest(
    val show: Boolean,
    val anchorBounds: androidx.compose.ui.geometry.Rect,
    val items: List<NativeMenuItem>,
    val dark: Boolean,
    val onDismissRequest: () -> Unit,
    val onDismissFinished: (() -> Unit)? = null,
    val collapseOnSelection: Boolean = true
)

internal val LocalNativePopupRenderer = compositionLocalOf<(@Composable (NativePopupRequest) -> Boolean)?> { null }
