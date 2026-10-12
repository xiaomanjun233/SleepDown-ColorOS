package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.ImageView
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.xiaomanjun.sleepdownschedule.app.ui.AddMenuAction

/** Provided only in parallel mode; ordinary mode keeps its current anchored menu. */
internal val LocalColorOsOverflowMenu = compositionLocalOf<State<List<AddMenuAction>>?> { null }

internal class NativeOverflowMenu(private val source: ColorOsNativeUi.Source, private val context: Context,
    private val host: Context, private val neutral: Boolean, private val foreground: Int?,
    private val onDismiss: (() -> Unit)? = null) {
    private val type = source.load("com.coui.appcompat.poplist.COUIPopupListWindow")
    private val itemType = source.load("com.coui.appcompat.poplist.PopupListItem")
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
            applyForeground(window.contentView)
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

    private fun applyForeground(view: View, textColor: Int? = foreground, iconColor: Int? = textColor) {
        if (view is TextView) {
            textColor?.let(view::setTextColor)
            iconColor?.let { color -> view.compoundDrawablesRelative.forEach { it?.mutate()?.setTint(color) } }
        }
        // COUI's adapter applies an ImageView tint after binding the supplied drawable.
        // Override that tint as well, after the native menu has attached and laid out.
        if (view is ImageView && iconColor != null) {
            view.clearColorFilter()
            view.imageTintList = ColorStateList.valueOf(iconColor)
            view.drawable?.mutate()?.setTint(iconColor)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            val action = if (view is AdapterView<*>) actions.getOrNull(view.getPositionForView(child)) else null
            applyForeground(child, action?.textTint?.takeIf { it.isSpecified }?.toArgb() ?: textColor,
                action?.iconTint?.takeIf { it.isSpecified }?.toArgb() ?: iconColor)
        }
    }

    private fun applyButtonMaterial() {
        val params = source.weatherButtonBlurParams(context)
        val base = source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
        val ui = source.load("com.coui.appcompat.uiutil.UIUtil")
        fun layer(method: String): Any = ui.getMethod("colorToFloats", Int::class.javaPrimitiveType)
            .invoke(null, base.getMethod(method, Context::class.java).invoke(params, context))
        val blend = layer("getColorLayer1")
        val mix = layer("getColorLayer2")
        for (field in listOf("mMainMenuWrapper", "mSubMenuWrapper")) {
            val wrapper = type.getDeclaredField(field).apply { isAccessible = true }.get(window) as View
            val builder = wrapper.javaClass.getDeclaredField("mBackgroundBlurBuilder")
                .apply { isAccessible = true }.get(wrapper)
            val builderType = builder.javaClass
            // Keep the native cross-window sampling and animated corners. Mode 4 is COUI's
            // neutral mix/blend material; both theme branches use the button's color layers.
            builderType.getMethod("setMode", Int::class.javaPrimitiveType).invoke(builder, 4)
            // The window's radius is a compositor parameter, not the View RenderEffect sigma.
            // Keep Weather/COUI's native radius (160); only match the neutral color layers.
            for (theme in listOf("Light", "Dark")) {
                builderType.getMethod("setMixColor$theme", FloatArray::class.java).invoke(builder, mix)
                builderType.getMethod("setBlendColor$theme", FloatArray::class.java).invoke(builder, blend)
            }
            if (wrapper.isAttachedToWindow) builderType.getMethod("refreshBlurParams").invoke(builder)
        }
        Log.i("SleepDownCOUI", "Overflow neutral colors matched; native window blur retained")
    }

    fun show(anchor: View, entries: List<AddMenuAction>) {
        // COUI's isShowing() becomes false at the start of exit, while its root is still attached.
        if (window.isShowing || window.contentView.isAttachedToWindow || pending != null || entries.isEmpty()) return
        this.anchor = anchor
        dismissed = false
        actions = entries
        val constructor = itemType.getConstructor(Drawable::class.java, String::class.java,
            Boolean::class.javaPrimitiveType)
        val items = entries.map { action ->
            val icon = action.iconRes?.let { ContextCompat.getDrawable(host, colorOsHomeIconResource(it))?.mutate() }
            icon?.setTintList(action.iconTint?.takeIf { it.isSpecified }?.toArgb()?.let(ColorStateList::valueOf)
                ?: foreground?.let(ColorStateList::valueOf)
                ?: source.color(context, "couiColorLabelPrimary"))
            constructor.newInstance(icon, action.label, true)
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
