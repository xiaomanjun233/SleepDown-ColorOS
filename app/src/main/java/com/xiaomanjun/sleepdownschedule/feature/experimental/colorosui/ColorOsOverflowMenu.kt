package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.AdapterView
import android.widget.PopupWindow
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.core.content.ContextCompat
import com.xiaomanjun.sleepdownschedule.app.ui.AddMenuAction

/** Provided only in parallel mode; ordinary mode keeps its current anchored menu. */
internal val LocalColorOsOverflowMenu = compositionLocalOf<State<List<AddMenuAction>>?> { null }

internal class NativeOverflowMenu(source: ColorOsNativeUi.Source, context: Context, private val host: Context) {
    private val type = source.load("com.coui.appcompat.poplist.COUIPopupListWindow")
    private val itemType = source.load("com.coui.appcompat.poplist.PopupListItem")
    private val window = type.getConstructor(Context::class.java).newInstance(context) as PopupWindow
    private var actions: List<AddMenuAction> = emptyList()
    private var pending: (() -> Unit)? = null

    init {
        type.getMethod("setUseBackgroundBlur", Boolean::class.javaPrimitiveType).invoke(window, true)
        type.getMethod("setOnItemClickListener", AdapterView.OnItemClickListener::class.java)
            .invoke(window, AdapterView.OnItemClickListener { _, _, position, _ ->
                pending = actions.getOrNull(position)?.onClick
                window.dismiss()
            })
        window.setOnDismissListener {
            val action = pending
            pending = null
            action?.invoke()
        }
    }

    fun show(anchor: View, entries: List<AddMenuAction>) {
        if (window.isShowing || entries.isEmpty()) return
        actions = entries
        val constructor = itemType.getConstructor(Drawable::class.java, String::class.java,
            Boolean::class.javaPrimitiveType)
        val items = entries.map { action ->
            constructor.newInstance(action.iconRes?.let { ContextCompat.getDrawable(host, it) }, action.label, true)
        }
        type.getMethod("setItemList", List::class.java).invoke(window, items)
        type.getMethod("show", View::class.java).invoke(window, anchor)
    }

    fun dispose() {
        pending = null
        window.setOnDismissListener(null)
        type.getMethod("forceDismiss").invoke(window)
    }
}
