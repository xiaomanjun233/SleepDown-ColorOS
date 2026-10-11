package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.DrawableWrapper
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
import com.xiaomanjun.sleepdownschedule.R
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
    val listener: Any,
    private val icons: List<Drawable>,
    private val source: ColorOsNativeUi.Source,
    private val context: Context
) {
    fun refreshAppearance() {
        // refresh() leaves the expansion view, metaball chain and active springs bound.
        view.javaClass.getMethod("refresh").invoke(view)
        val colors = dockIconColors(source, context)
        icons.forEach { it.setTintList(colors) }
        view.javaClass.getMethod("setItemsTitleTextColor", ColorStateList::class.java).invoke(view, colors)
        tintLight()
        Log.i("SleepDownCOUI", "Dock appearance refreshed in place: view=${System.identityHashCode(view)}, " +
            "night=${context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK}")
    }

    fun tintLight() {
        val base = "com.coui.appcompat.bottomfloatingnavigationbar.BaseBottomFloatingMenuView"
        val menuType = source.load(base)
        for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (menuType.isInstance(child)) source.tintSpotLight(child, base, "mSpotLightEffectDrawable")
        }
    }

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

private fun dockIconColors(source: ColorOsNativeUi.Source, context: Context): ColorStateList {
    val variant = source.color(context, "couiColorLabelPrimaryVariant").defaultColor
    val disabled = source.color(context, "couiColorLabelTertiary").defaultColor
    return ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),
        intArrayOf(android.R.attr.state_checked), intArrayOf(android.R.attr.state_selected),
        intArrayOf(android.R.attr.state_pressed), intArrayOf()),
        intArrayOf(disabled, ColorOsNativeUi.HomeAccent, ColorOsNativeUi.HomeAccent, variant, variant))
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
    // COUI tints its labels, but accepts custom icons without applying a tint to them.
    val iconColors = dockIconColors(source, context)
    val icons = mutableListOf<Drawable>()
    val items = tabs.mapIndexed { index, (icon, title) ->
        val builder = builderType.getConstructor().newInstance()
        builderType.getMethod("setId", Int::class.javaPrimitiveType).invoke(builder, index + 1)
        builderType.getMethod("setTitle", String::class.java).invoke(builder, title)
        builderType.getMethod("setDescription", CharSequence::class.java).invoke(builder, title)
        val currentIcon = when (icon) {
            R.drawable.ic_day_view -> R.drawable.ic_coloros_dock_today
            R.drawable.ic_week_view -> R.drawable.ic_coloros_dock_week
            // This XML has the same path as the original Compose Icons.Default.Settings.
            else -> icon
        }
        val drawable = requireNotNull(ContextCompat.getDrawable(host, currentIcon)).mutate()
        drawable.setTintList(iconColors)
        icons += drawable
        builderType.getMethod("setIcon", android.graphics.drawable.Drawable::class.java)
            .invoke(builder, drawable)
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
    type.getMethod("setItemsTitleTextColor", ColorStateList::class.java).invoke(bar, iconColors)
    bar.clipChildren = false
    bar.clipToPadding = false
    return NativeDock(bar, type.getMethod("getSelectedItemId"),
        type.getMethod("setSelectedItemId", Int::class.javaPrimitiveType), setListener, listener,
        icons, source, context).also { dock ->
        bar.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { view.post { dock.tintLight() } }
            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}

@Composable
internal fun rememberNativeContext(dark: Boolean): Pair<ColorOsNativeUi.Source, Context>? {
    val host = LocalContext.current
    val configuration = LocalConfiguration.current
    val source = remember(host) { ColorOsNativeUi.source(host) }
    return remember(source, host, configuration) {
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
            if (source.updateTheme(context, dark)) dock.refreshAppearance()
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
            // Gallery's toolbar icon items use this delegate, rather than the tiny text-button style.
            val type = source.load(ColorOsNativeUi.ToolbarMaterial)
            val drawSpotLight = type.getMethod("drawSpotLight", Canvas::class.java)
            var effect: Any? = null
            val view = object : ImageButton(context) {
                override fun draw(canvas: Canvas) {
                    super.draw(canvas)
                    effect?.let { drawSpotLight.invoke(it, canvas) }
                }
            }
            view.minimumWidth = 0
            view.minimumHeight = 0
            view.setPadding(0, 0, 0, 0)
            view.scaleType = ImageView.ScaleType.CENTER
            val icon = requireNotNull(ContextCompat.getDrawable(host, iconRes)).mutate()
            icon.setTintList(source.color(context, "couiColorLabelPrimary"))
            val iconSize = (20 * context.resources.displayMetrics.density).toInt()
            view.setImageDrawable(object : DrawableWrapper(icon) {
                override fun getIntrinsicWidth(): Int = iconSize
                override fun getIntrinsicHeight(): Int = iconSize
            })
            effect = type.getMethod("forMenuItem", View::class.java).invoke(null, view)
            val state = source.load("com.coui.appcompat.toolbar.COUIToolbar\$MaterialEffectState")
            val follow = state.getMethod("getValue").invoke(state.getField("FOLLOW_SYSTEM").get(null))
            type.getMethod("setUseMaterialEffectAttr", Int::class.javaPrimitiveType).invoke(effect, follow)
            val materialType = source.load("com.coui.appcompat.toolbar.COUIMaterialEffectType")
                .getField("TYPE_DEFAULT").getInt(null)
            type.getMethod("setMaterialEffectType", Int::class.javaPrimitiveType).invoke(effect, materialType)
            NativeIconButton(view, requireNotNull(effect), type.getMethod("detach"), source).also { button ->
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) { view.post { button.refreshAppearance() } }
                    override fun onViewDetachedFromWindow(view: View) = Unit
                })
            }
        }
    } ?: return false
    key(context, iconRes) {
        AndroidView(factory = { button.view }, modifier = modifier, update = { view ->
            if (source.updateTheme(context, dark)) button.refreshAppearance()
            view.contentDescription = contentDescription
            view.isSelected = selected
            // Resting return-anchor copies retain the normal material, without accepting input.
            view.setOnClickListener(if (interactive) View.OnClickListener { callback.value() } else null)
            view.isClickable = interactive
            view.isFocusable = interactive
        })
    }
    DisposableEffect(button) { onDispose { button.dispose() } }
    return true
}

private class NativeIconButton(val view: ImageButton, val effect: Any, val detach: Method,
    private val source: ColorOsNativeUi.Source) {
    fun refreshAppearance() {
        effect.javaClass.getMethod("refreshRipple", Context::class.java).invoke(effect, view.context)
        view.drawable.setTintList(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(ColorOsNativeUi.HomeAccent, source.color(view.context, "couiColorLabelPrimary").defaultColor)))
        source.tintSpotLight(effect, ColorOsNativeUi.ToolbarMaterial, "mSpotLight")
        val materialState = listOf("mMaterialType", "mBlurAllowed", "mBlurOn", "mStrokeAllowed", "mStrokeOn")
            .joinToString { name ->
                val field = effect.javaClass.getDeclaredField(name).apply { isAccessible = true }
                "$name=${field.get(effect)}"
            }
        Log.i("SleepDownCOUI", "Toolbar material refreshed: size=${view.width}x${view.height}, " +
            "background=${view.background?.javaClass?.simpleName}, $materialState")
    }

    fun dispose() {
        view.setOnClickListener(null)
        detach.invoke(effect)
    }
}
