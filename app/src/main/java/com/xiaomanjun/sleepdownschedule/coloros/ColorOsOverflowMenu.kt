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
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.core.content.ContextCompat
import com.xiaomanjun.sleepdownschedule.app.ui.AddMenuAction

/** Provided only in parallel mode; ordinary mode keeps its current anchored menu. */
internal val LocalColorOsOverflowMenu = compositionLocalOf<State<List<AddMenuAction>>?> { null }

internal class NativeOverflowMenu(private val source: ColorOsNativeUi.Source, private val context: Context,
    private val host: Context, private val neutral: Boolean, private val foreground: Int?) {
    private val type = source.load("com.coui.appcompat.poplist.COUIPopupListWindow")
    private val itemType = source.load("com.coui.appcompat.poplist.PopupListItem")
    private val window = type.getConstructor(Context::class.java).newInstance(context) as PopupWindow
    private var actions: List<AddMenuAction> = emptyList()
    private var pending: (() -> Unit)? = null
    private var anchor: View? = null
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

    private fun applyForeground(view: View) {
        val color = foreground ?: return
        if (view is TextView) view.setTextColor(color)
        if (view is ViewGroup) for (index in 0 until view.childCount) applyForeground(view.getChildAt(index))
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
        actions = entries
        val constructor = itemType.getConstructor(Drawable::class.java, String::class.java,
            Boolean::class.javaPrimitiveType)
        val items = entries.map { action ->
            val icon = action.iconRes?.let { ContextCompat.getDrawable(host, colorOsHomeIconResource(it))?.mutate() }
            icon?.setTintList(foreground?.let(ColorStateList::valueOf)
                ?: source.color(context, "couiColorLabelPrimary"))
            constructor.newInstance(icon, action.label, true)
        }
        type.getMethod("setItemList", List::class.java).invoke(window, items)
        type.getMethod("show", View::class.java).invoke(window, anchor)
    }

    private fun runPendingAction() {
        val action = pending
        pending = null
        if (anchor?.isAttachedToWindow == true) action?.invoke()
    }

    fun dispose() {
        pending = null
        type.getMethod("setOnMainMenuAnimationListener", animationListenerType).invoke(window, null)
        type.getMethod("forceDismiss").invoke(window)
        anchor = null
    }
}
