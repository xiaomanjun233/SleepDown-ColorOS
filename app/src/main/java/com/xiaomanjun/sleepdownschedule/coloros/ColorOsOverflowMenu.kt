package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.PopupWindow
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import com.xiaomanjun.sleepdownschedule.app.ui.AddMenuAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuItem

/** Provided only in parallel mode; ordinary mode keeps its current anchored menu. */
internal val LocalColorOsOverflowMenu = compositionLocalOf<State<List<AddMenuAction>>?> { null }

internal class NativeOverflowMenu(private val source: ColorOsNativeUi.Source, private val context: Context,
    private val host: Context, private val neutral: Boolean, private val foreground: Int?,
    private val onDismiss: (() -> Unit)? = null) {
    private val type = source.load("com.coui.appcompat.poplist.COUIPopupListWindow")
    private val window = type.getConstructor(Context::class.java).newInstance(context) as PopupWindow
    private var actions: List<AddMenuAction> = emptyList()
    private var pending: (() -> Unit)? = null
    private var anchor: View? = null
    private var dismissed = true
    private var dismissWaiter: (() -> Unit)? = null
    private val animationListenerType = source.load("${type.name}\$OnMainMenuAnimationListener")

    init {
        source.bindFollowLightIntensity(window.contentView, multiplier = 3f, afterMaterialApplied = {
            if (neutral) applyButtonMaterial()
        })
        type.getMethod("setUseBackgroundBlur", Boolean::class.javaPrimitiveType).invoke(window, true)
        // WindowManager resolves animation resources against our package, not the vendor APK.
        // The menu root already owns COUI's enter/exit springs; avoid a second window animation.
        window.animationStyle = 0
        window.setOnDismissListener { anchor?.post { runPendingAction() } }
        type.getMethod("setMenuWidth", Int::class.javaPrimitiveType).invoke(window,
            context.resources.getDimensionPixelSize(source.resource("coui_popup_list_window_width_with_icon", "dimen")))
        type.getMethod("setMenuMaxWidth", Int::class.javaPrimitiveType).invoke(window,
            context.resources.getDimensionPixelSize(source.resource("coui_popup_list_window_max_width", "dimen")))
        type.getMethod("setOnItemClickListener", AdapterView.OnItemClickListener::class.java)
            .invoke(window, AdapterView.OnItemClickListener { _, _, position, _ ->
                if (window.isShowing && pending == null) {
                    pending = actions.getOrNull(position)?.onClick
                    window.dismiss()
                }
            })
        type.getMethod("setOnMainMenuAnimationListener", animationListenerType).invoke(window,
            source.listener(animationListenerType.name) { name, _ ->
                Log.i("SleepDownCOUI", "Overflow animation: $name")
                // An enter spring can be canceled by exit; keep a selected action until actual exit.
                if (name == "onMainMenuExited") anchor?.post { runPendingAction() }
                null
            })
        window.contentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) { anchor?.post { runPendingAction() } }
        })
    }

    private fun applyButtonMaterial() {
        for (field in listOf("mMainMenuWrapper", "mSubMenuWrapper")) {
            val wrapper = type.getDeclaredField(field).apply { isAccessible = true }.get(window) as View
            source.applyNeutralMenuMaterial(context, wrapper)
        }
        Log.i("SleepDownCOUI", "Overflow neutral colors matched; native window blur retained")
    }

    fun show(anchor: View, entries: List<AddMenuAction>) {
        // COUI's isShowing() becomes false at the start of exit, while its root is still attached.
        if (window.isShowing || window.contentView.isAttachedToWindow || pending != null || entries.isEmpty()) return
        this.anchor = anchor
        dismissed = false
        actions = entries
        val items = entries.map { action ->
            source.createMenuItem(context, host, NativeMenuItem(action.label, action.iconRes,
                foreground = action.textTint?.takeIf { it.isSpecified }?.toArgb() ?: foreground,
                iconTint = action.iconTint?.takeIf { it.isSpecified }?.toArgb() ?: foreground))
        }
        type.getMethod("setItemList", List::class.java).invoke(window, items)
        type.getMethod("show", View::class.java).invoke(window, anchor)
    }

    private fun runPendingAction() {
        if (dismissed) return
        dismissed = true
        val action = pending
        pending = null
        val waiter = dismissWaiter
        dismissWaiter = null
        waiter?.invoke()
        if (anchor?.isAttachedToWindow == true) {
            if (action != null) action() else onDismiss?.invoke()
        }
    }

    fun dismiss(afterDismiss: () -> Unit) {
        if (dismissed) { afterDismiss(); return }
        dismissWaiter = afterDismiss
        window.dismiss()
    }

    fun dispose() {
        pending = null
        dismissed = true
        dismissWaiter?.invoke()
        dismissWaiter = null
        type.getMethod("setOnMainMenuAnimationListener", animationListenerType).invoke(window, null)
        type.getMethod("forceDismiss").invoke(window)
        anchor = null
    }
}
