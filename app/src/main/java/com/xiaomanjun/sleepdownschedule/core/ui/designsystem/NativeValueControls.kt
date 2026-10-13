package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

internal data class NativeSliderRequest(
    val value: () -> Float, val range: ClosedFloatingPointRange<Float>,
    val onPreview: (Float) -> Unit, val onPreviewMode: (Boolean) -> Unit,
    val onCommit: (Float) -> Unit, val modifier: Modifier,
    val snapValue: Float? = null, val enabled: Boolean = true
)

internal data class NativeSwitchRequest(
    val checked: Boolean, val onCheckedChange: (Boolean) -> Unit,
    val modifier: Modifier, val enabled: Boolean = true, val compact: Boolean = false
)

internal data class NativeSegmentRequest(
    val labels: List<String>, val selected: Int, val onSelected: (Int) -> Unit,
    val modifier: Modifier, val dark: Boolean
)

internal val LocalNativeSliderRenderer = compositionLocalOf<(@Composable (NativeSliderRequest) -> Boolean)?> { null }
internal val LocalNativeSwitchRenderer = compositionLocalOf<(@Composable (NativeSwitchRequest) -> Boolean)?> { null }
internal val LocalNativeSegmentRenderer = compositionLocalOf<(@Composable (NativeSegmentRequest) -> Boolean)?> { null }

/** Native touch callbacks retain the existing preview/commit/cancel transaction. */
internal class NativeSliderSession(private val request: () -> NativeSliderRequest) {
    var tracking = false
        private set
    private var initial = 0f
    private var pending = 0f
    fun start() {
        if (tracking) return
        tracking = true
        initial = request().value().coerceIn(request().range)
        pending = initial
        request().onPreviewMode(true)
    }
    fun change(value: Float) {
        val current = request()
        pending = value.coerceIn(current.range)
        current.onPreview(pending)
        // Keyboard and accessibility changes do not start a touch tracking session.
        if (!tracking) current.onCommit(pending)
    }
    fun finish(cancelled: Boolean) {
        if (!tracking) return
        tracking = false
        if (cancelled) request().onPreview(initial) else request().onCommit(pending)
        request().onPreviewMode(false)
    }
}
