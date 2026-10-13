package com.xiaomanjun.sleepdownschedule.coloros

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuItem
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativePopupRequest
import kotlin.math.roundToInt

/** A real COUI popup window; the transparent anchor shares the vendor resource context. */
@Composable
internal fun ColorOsPopup(request: NativePopupRequest): Boolean {
    val host = LocalContext.current
    if (remember(host) { ColorOsNativeUi.source(host) } == null) return false
    // COUI exposes a main menu and one submenu. Keep deeper trees with their existing host.
    if (request.items.any { item -> item.children.any { it.children.isNotEmpty() } }) return false
    var mounted by remember { mutableStateOf(false) }
    if (request.show) mounted = true
    if (!mounted) return true
    val native = rememberNativeContext(request.dark) ?: return false
    val composeView = LocalView.current
    val root = composeView.rootView as? ViewGroup ?: return false
    val latest = rememberUpdatedState(request)
    val finish = rememberUpdatedState<() -> Unit> {
        if (mounted) {
            mounted = false
            latest.value.onDismissRequest()
            latest.value.onDismissFinished?.invoke()
        }
    }
    val menu = remember(native, root) {
        ColorOsNativeUi.prepare("create system popup") {
            NativeOverflowMenu(native.first, native.second, host, neutral = false, foreground = null,
                onDismiss = { finish.value() })
        }
    } ?: return false
    var failed by remember(menu) { mutableStateOf(false) }
    if (failed) return false
    val anchor = remember(menu) { View(native.second).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    } }
    DisposableEffect(menu, root) {
        root.overlay.add(anchor)
        onDispose { menu.dispose(); root.overlay.remove(anchor) }
    }
    LaunchedEffect(menu, request.show, request.anchorBounds) {
        if (!request.show) {
            menu.dismiss { finish.value() }
        } else if (request.anchorBounds.width > 0f && request.anchorBounds.height > 0f) {
            val composeLocation = IntArray(2).also(composeView::getLocationOnScreen)
            val rootLocation = IntArray(2).also(root::getLocationOnScreen)
            val bounds = request.anchorBounds
            val x = composeLocation[0] - rootLocation[0]
            val y = composeLocation[1] - rootLocation[1]
            anchor.layout((bounds.left + x).roundToInt(), (bounds.top + y).roundToInt(),
                (bounds.right + x).roundToInt(), (bounds.bottom + y).roundToInt())
            fun actionItem(item: NativeMenuItem): NativeMenuItem = item.copy(
                children = item.children.map(::actionItem),
                onClick = {
                    if (latest.value.collapseOnSelection) finish.value()
                    item.onClick()
                }
            )
            if (ColorOsNativeUi.prepare("show system popup") {
                    menu.showItems(anchor, latest.value.items.map(::actionItem), latest.value.collapseOnSelection)
                    true
                } != true) failed = true
        }
    }
    return true
}
