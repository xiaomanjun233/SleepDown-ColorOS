package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.CheckBox
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*

/** Shared with the real overflow window, so tint/selection rules cannot drift between hosts. */
internal fun ColorOsNativeUi.Source.createMenuItem(context: Context, host: Context, item: NativeMenuItem): Any {
    val type = load("com.coui.appcompat.poplist.PopupListItem")
    val foreground = item.foreground?.let(ColorStateList::valueOf) ?: color(context, "couiColorLabelPrimary")
    val icon = item.iconRes?.let { ContextCompat.getDrawable(host, colorOsHomeIconResource(it))?.mutate() }
    icon?.setTintList(item.iconTint?.let(ColorStateList::valueOf) ?: foreground)
    return type.getConstructor(Drawable::class.java, String::class.java, Boolean::class.javaPrimitiveType)
        .newInstance(icon, item.title, item.enabled).also {
            type.getMethod("setForceTint", Int::class.javaPrimitiveType).invoke(it, 0)
            type.getMethod("setTitleColorList", ColorStateList::class.java).invoke(it, foreground)
            type.getMethod("setCheckable", Boolean::class.javaPrimitiveType).invoke(it, item.checkable)
            type.getMethod("setChecked", Boolean::class.javaPrimitiveType).invoke(it, item.selected)
            item.description?.let { value -> type.getMethod("setDescription", String::class.java).invoke(it, value) }
            if (item.children.isNotEmpty()) type.getMethod("setSubMenuItemList", ArrayList::class.java)
                .invoke(it, ArrayList(item.children.map { child -> createMenuItem(context, host, child) }))
        }
}

internal fun ColorOsNativeUi.Source.applyNeutralMenuMaterial(context: Context, wrapper: View) {
    val params = weatherButtonBlurParams(context)
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val ui = load("com.coui.appcompat.uiutil.UIUtil")
    fun layer(method: String): Any = ui.getMethod("colorToFloats", Int::class.javaPrimitiveType)
        .invoke(null, base.getMethod(method, Context::class.java).invoke(params, context))
    val builder = wrapper.javaClass.getDeclaredField("mBackgroundBlurBuilder")
        .apply { isAccessible = true }.get(wrapper)
    val type = builder.javaClass
    type.getMethod("setMode", Int::class.javaPrimitiveType).invoke(builder, 4)
    for (theme in listOf("Light", "Dark")) {
        type.getMethod("setMixColor$theme", FloatArray::class.java).invoke(builder, layer("getColorLayer2"))
        type.getMethod("setBlendColor$theme", FloatArray::class.java).invoke(builder, layer("getColorLayer1"))
    }
    if (wrapper.isAttachedToWindow) type.getMethod("refreshBlurParams").invoke(builder)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ColorOsMenuSurface(request: NativeMenuSurfaceRequest, content: @Composable () -> Unit): Boolean {
    val value = request.surface
    val native = rememberNativeContext(value.dark) ?: return false
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val latest = rememberUpdatedState(request)
    val binding = remember(native, request.neutral, value.dark) {
        ColorOsNativeUi.prepare("create embedded native menu panel") {
            native.first.updateTheme(native.second, value.dark)
            NativeMenuPanel(native.first, native.second, request.neutral)
        }
    } ?: return false
    Box(value.modifier.graphicsLayer {
        val surface = latest.value.surface
        shape = surface.shapeProvider?.invoke() ?: surface.shape
        clip = true
    }.then(if (request.touchSource == null) Modifier.motionEventSpy(binding::followTouch) else Modifier)) {
        if (value.renderSurface) AndroidView(factory = { binding.view }, modifier = Modifier.matchParentSize()
            .graphicsLayer { alpha = latest.value.surface.surfaceAlpha() }
            .drawWithContent {
                val surface = latest.value.surface
                val shape = surface.shapeProvider?.invoke() ?: surface.shape
                colorOsMaterialCorners(shape, Size(size.width, size.height), density, direction)?.let {
                    binding.setRadius(it.radius)
                }
                drawContent()
            })
        CompositionLocalProvider(LocalNativeMaterialSurfaceActive provides true, content = content)
    }
    DisposableEffect(binding, request.touchSource) {
        request.touchSource?.onTouch = binding::followTouch
        onDispose { request.touchSource?.onTouch = null; binding.clearTouch() }
    }
    return true
}

/** The same RoundFrameLayout as COUIPopupListWindow, without a second popup/window animator. */
private class NativeMenuPanel(source: ColorOsNativeUi.Source, context: Context, neutral: Boolean) {
    private val type = source.load("com.coui.appcompat.poplist.RoundFrameLayout")
    val view = type.getConstructor(Context::class.java, AttributeSet::class.java)
        .newInstance(context, null) as FrameLayout
    private val radiusMethod = type.getMethod("setRoundCornerRadius", Float::class.javaPrimitiveType)
    private var radius = -1f
    private val location = IntArray(2)
    init {
        // Match COUIPopupListWindow's fallback drawable; initUseBackgroundBlur manages its visibility.
        val attrs = context.theme.obtainStyledAttributes(intArrayOf(source.resource("couiPopupWindowBackground", "attr")))
        view.background = try {
            attrs.getDrawable(0) ?: context.getDrawable(source.resource("coui_popup_window_background", "drawable"))
        } finally { attrs.recycle() }
        val level = source.load("com.coui.appcompat.uiutil.UIUtil").getField("ANIM_LEVEL_SUPPORT_BLUR_MIN").get(null)
        type.getMethod("initUseBackgroundBlur", Boolean::class.javaPrimitiveType,
            source.load("com.coui.appcompat.uiutil.AnimLevel")).invoke(view, true, level)
        type.getMethod("setSpotLightTouchEnabled", Boolean::class.javaPrimitiveType).invoke(view, true)
        view.isClickable = false
        view.isFocusable = false
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        source.bindFollowLightIntensity(view, multiplier = 3f, afterMaterialApplied = {
            if (neutral) source.applyNeutralMenuMaterial(context, view)
        })
        if (neutral) source.applyNeutralMenuMaterial(context, view)
    }
    fun setRadius(value: Float) {
        if (radius != value) {
            radiusMethod.invoke(view, value)
            type.getMethod("updateBlurCornerRadius").invoke(view)
            radius = value
        }
    }
    fun clearTouch() { type.getMethod("clearSpotLightState").invoke(view) }
    fun followTouch(event: MotionEvent) {
        view.getLocationOnScreen(location)
        val local = MotionEvent.obtain(event)
        try {
            local.setLocation(event.rawX - location[0], event.rawY - location[1])
            view.dispatchTouchEvent(local)
        } finally { local.recycle() }
    }
}

@Composable
internal fun ColorOsMenuRow(request: NativeMenuRowRequest): Boolean {
    val native = rememberNativeContext(request.dark) ?: return false
    val host = LocalContext.current
    val binding = remember(native, host, request.item, request.first, request.last, request.dark) {
        ColorOsNativeUi.prepare("create native popup menu item") {
            val source = native.first
            val context = native.second
            source.updateTheme(context, request.dark)
            val item = source.createMenuItem(context, host, request.item)
            val adapter = source.load("com.coui.appcompat.poplist.DefaultAdapter")
                .getConstructor(Context::class.java, List::class.java).newInstance(context, listOf(item)) as BaseAdapter
            // Input stays with the host's existing slide-selection and cascading controller.
            val container = object : FrameLayout(context) {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
            }.apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS }
            val row = adapter.getView(0, null, container)
            val edge = context.resources.getDimensionPixelSize(source.resource("coui_popup_list_padding_vertical", "dimen"))
            val trimTop = if (request.first) 0 else edge
            val trimBottom = if (request.last) 0 else edge
            row.setPaddingRelative(row.paddingStart, (row.paddingTop - trimTop).coerceAtLeast(0),
                row.paddingEnd, (row.paddingBottom - trimBottom).coerceAtLeast(0))
            row.minimumHeight = (row.minimumHeight - trimTop - trimBottom).coerceAtLeast(0)
            container.addView(row, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT))
            source.bindFollowLightIntensity(row, multiplier = 3f)
            val indicator = if (request.item.children.isNotEmpty()) findMenuIndicator(row) else null
            Triple(container, row, indicator)
        }
    } ?: return false
    val latest = rememberUpdatedState(request)
    // Popup hosts ask for intrinsic sizes. AndroidView alone cannot answer those queries.
    val policy = remember(binding) { NativeMenuMeasurePolicy(binding.first) }
    Layout(content = {
        AndroidView(factory = { binding.first }, update = {
            binding.second.isPressed = request.pressed && request.item.enabled
        }, modifier = Modifier.drawWithContent {
            binding.third?.rotation = latest.value.indicatorRotation()
            drawContent()
        })
    }, modifier = latest.value.modifier, measurePolicy = policy)
    return true
}

private fun findMenuIndicator(view: View): CheckBox? {
    if (view is CheckBox) return view
    if (view is android.view.ViewGroup) for (index in 0 until view.childCount) {
        findMenuIndicator(view.getChildAt(index))?.let { return it }
    }
    return null
}

internal class NativeMenuMeasurePolicy(private val view: View) : MeasurePolicy {
    private fun nativeSize(width: Int): Pair<Int, Int> {
        view.measure(View.MeasureSpec.makeMeasureSpec(if (width == Constraints.Infinity) 0 else width,
            if (width == Constraints.Infinity) View.MeasureSpec.UNSPECIFIED else View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return view.measuredWidth to view.measuredHeight
    }
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val child = measurables.single().measure(constraints)
        return layout(child.width, child.height) { child.place(0, 0) }
    }
    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = nativeSize(Constraints.Infinity).first
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) = nativeSize(Constraints.Infinity).first
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = nativeSize(width).second
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = nativeSize(width).second
}
