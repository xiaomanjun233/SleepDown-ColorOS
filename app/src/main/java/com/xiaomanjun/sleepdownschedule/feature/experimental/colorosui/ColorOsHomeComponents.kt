package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.lang.reflect.Method

private const val NavigationItem =
    "com.coui.appcompat.bottomfloatingnavigationbar.COUIBottomFloatingNavigationBarItem"
private const val NavigationMaterial =
    "com.coui.appcompat.bottomfloatingnavigationbar.BaseBottomFloatingMenuView\$MaterialEffectState"

internal class NativeDock(
    val view: ViewGroup,
    val getSelected: Method,
    val setSelected: Method,
    val setListener: Method,
    val listener: Any
) {
    fun select(index: Int) {
        val wanted = index + 1
        if (getSelected.invoke(view) == wanted) return
        // State synchronization is separate from the native user's touch/slide callbacks.
        setListener.invoke(view, null)
        try {
            setSelected.invoke(view, wanted)
        } finally {
            setListener.invoke(view, listener)
        }
    }

    fun dispose() { setListener.invoke(view, null) }
}

internal fun createNativeHomeDock(
    source: ColorOsNativeUi.Source,
    context: Context,
    host: Context,
    tabs: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
): NativeDock {
    val type = source.load(ColorOsNativeUi.FloatingNavigation)
    val bar = type.getConstructor(Context::class.java).newInstance(context) as ViewGroup
    val builderType = source.load("$NavigationItem\$Builder")
    val items = tabs.mapIndexed { index, (icon, title) ->
        val builder = builderType.getConstructor().newInstance()
        builderType.getMethod("setId", Int::class.javaPrimitiveType).invoke(builder, index + 1)
        builderType.getMethod("setTitle", String::class.java).invoke(builder, title)
        builderType.getMethod("setDescription", CharSequence::class.java).invoke(builder, title)
        builderType.getMethod("setIcon", android.graphics.drawable.Drawable::class.java)
            .invoke(builder, requireNotNull(ContextCompat.getDrawable(host, icon)).mutate())
        builderType.getMethod("build").invoke(builder)
    }
    type.getMethod("addItems", List::class.java).invoke(bar, items)
    type.getMethod("setSelectedItemId", Int::class.javaPrimitiveType)
        .invoke(bar, selectedIndex.coerceIn(tabs.indices) + 1)
    val listenerTypeName = "${ColorOsNativeUi.FloatingNavigation}\$OnItemSelectedListener"
    val setListener = type.getMethod("setOnItemSelectedListener", source.load(listenerTypeName))
    val listener = source.listener(listenerTypeName) { name, arguments ->
        if (name == "onNavigationItemSelected") {
            val item = requireNotNull(arguments[1])
            val id = item.javaClass.getMethod("getId").invoke(item) as Int
            if (id - 1 in tabs.indices) onSelect(id - 1)
        }
        null
    }
    setListener.invoke(bar, listener)
    source.material(bar, NavigationMaterial)
    bar.clipChildren = false
    bar.clipToPadding = false
    return NativeDock(bar, type.getMethod("getSelectedItemId"),
        type.getMethod("setSelectedItemId", Int::class.javaPrimitiveType), setListener, listener)
}

@Composable
internal fun rememberNativeContext(dark: Boolean): Pair<ColorOsNativeUi.Source, Context>? {
    val host = LocalContext.current
    val configuration = LocalConfiguration.current
    val source = remember(host) { ColorOsNativeUi.source(host) }
    return remember(source, host, dark, configuration) {
        source?.let { native ->
            ColorOsNativeUi.prepare("create the native theme") { native to native.themedContext(host, dark) }
        }
    }
}

/** Returns false only when native preparation failed, so the caller can use its existing UI. */
@Composable
internal fun ColorOsHomeDock(
    tabs: List<Pair<Int, String>>,
    selectedIndex: Int,
    dark: Boolean,
    modifier: Modifier,
    onSelect: (Int) -> Unit
): Boolean {
    val native = rememberNativeContext(dark) ?: return false
    val host = LocalContext.current
    val callback = rememberUpdatedState(onSelect)
    val source = native.first
    val context = native.second
    val dock = remember(source, context, tabs) {
        ColorOsNativeUi.prepare("create the floating navigation bar") {
            createNativeHomeDock(source, context, host, tabs, selectedIndex) { callback.value(it) }
        }
    } ?: return false
    key(context, tabs) {
        AndroidView(factory = { dock.view }, modifier = modifier, update = {
            dock.select(selectedIndex.coerceIn(tabs.indices))
        })
    }
    DisposableEffect(dock) {
        onDispose { dock.dispose() }
    }
    return true
}

@Composable
internal fun ColorOsHomeIconButton(
    iconRes: Int,
    contentDescription: String,
    dark: Boolean,
    interactive: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
): Boolean {
    val native = rememberNativeContext(dark) ?: return false
    val host = LocalContext.current
    val source = native.first
    val context = native.second
    val callback = rememberUpdatedState(onClick)
    val button = remember(source, context, iconRes) {
        ColorOsNativeUi.prepare("create the home icon button") {
            val type = source.load(ColorOsNativeUi.Button)
            val style = source.resource("couiTinySmallButtonSecondaryColorStyle", "attr")
            check(style != 0)
            val view = type.getConstructor(Context::class.java, AttributeSet::class.java, Int::class.javaPrimitiveType)
                .newInstance(context, null, style) as TextView
            source.material(view, "${ColorOsNativeUi.Button}\$MaterialEffectState")
            view.text = ""
            view.gravity = Gravity.CENTER
            view.minWidth = 0
            view.minimumWidth = 0
            view.minHeight = 0
            view.minimumHeight = 0
            view.setPadding(0, 0, 0, 0)
            val icon = requireNotNull(ContextCompat.getDrawable(host, iconRes)).mutate()
            icon.setTintList(view.textColors)
            val iconSize = (20 * context.resources.displayMetrics.density).toInt()
            icon.setBounds(0, 0, iconSize, iconSize)
            view.setCompoundDrawablesRelative(icon, null, null, null)
            view.compoundDrawablePadding = 0
            type.getMethod("setDrawableRadius", Int::class.javaPrimitiveType)
                .invoke(view, (21 * context.resources.displayMetrics.density).toInt())
            view
        }
    } ?: return false
    key(context, iconRes) {
        AndroidView(factory = { button }, modifier = modifier, update = { view ->
            view.contentDescription = contentDescription
            view.isSelected = selected
            // Resting return-anchor copies retain the normal material, without accepting input.
            view.setOnClickListener(if (interactive) View.OnClickListener { callback.value() } else null)
            view.isClickable = interactive
            view.isFocusable = interactive
        })
    }
    DisposableEffect(button) { onDispose { button.setOnClickListener(null) } }
    return true
}
