package com.xiaomanjun.sleepdownschedule.coloros

import android.view.View
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.app.ui.AddMenuAction
import kotlin.math.roundToInt

/** Reuse the home overflow window; this transparent View only supplies the course anchor. */
@Composable
internal fun ColorOsCourseShortcutMenu(
    session: Any, bounds: Rect, dark: Boolean, actions: List<AddMenuAction>, closing: Boolean,
    onDismiss: () -> Unit,
    bindDismiss: (((() -> Unit) -> Unit)?) -> Unit
): Boolean {
    val native = rememberNativeContext(dark) ?: return false
    val host = LocalContext.current
    val density = LocalDensity.current
    val style = LocalColorOsHomeChromeStyle.current
    val latestDismiss = rememberUpdatedState(onDismiss)
    val latestActions = rememberUpdatedState(actions)
    val latestClosing = rememberUpdatedState(closing)
    val menu = remember(native, session, style) {
        ColorOsNativeUi.prepare("create native course shortcut") {
            NativeOverflowMenu(native.first, native.first.themedContext(host, style?.dark ?: dark), host,
                neutral = style?.weatherMaterial == true, foreground = style?.foreground,
                onDismiss = { latestDismiss.value() })
        }
    } ?: return false
    val anchor = remember(menu) { View(host).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    } }
    val shown = remember(menu) { booleanArrayOf(false) }
    val active = remember(menu) { booleanArrayOf(true) }
    AndroidView(factory = { anchor }, modifier = Modifier
        .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
        .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
        .onGloballyPositioned {
            if (!shown[0] && anchor.isAttachedToWindow && anchor.width > 0 && anchor.height > 0) {
                shown[0] = true
                anchor.post {
                    if (active[0] && !latestClosing.value && anchor.isAttachedToWindow) {
                        menu.show(anchor, latestActions.value)
                    }
                }
            }
        })
    DisposableEffect(menu) {
        bindDismiss(menu::dismiss)
        onDispose { active[0] = false; bindDismiss(null); menu.dispose() }
    }
    return true
}
