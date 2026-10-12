package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Outline as AndroidOutline
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCourseCardRequest
import java.lang.reflect.Method

private const val CourseMaterialPreference = "coloros_course_material"
private const val CourseMaterialEnabled = "enabled"
private const val CourseTintStrength = "tint_strength"
private const val MaterialEffect = "com.coui.appcompat.COUIMaterialEffect"
private const val BlurType = "com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType"
private const val CourseBlurPreset = "TYPE_CONTENT_TRANSPARENT_BUTTON"
// BaseBottomFloatingMenuView's RenderNode stroke and follow-light presets. Geometry still
// comes from the course's outline, never the Dock's capsule corner radius.
private const val CourseStrokePreset = "TYPE_FRAMEWORK_CAPSULE_3"
private const val CourseLightPreset = "TYPE_OPAQUE_MEDIUM_2"
private const val StrokeType = "com.coui.appcompat.COUIMaterialStrokeEffect\$StrokeEffectType"
private const val LightDrawable = "com.coui.appcompat.spotlight.COUISpotLightEffectDrawable"
private const val LightType = "com.coui.appcompat.spotlight.COUISpotLightEffect\$SpotLightType"

/** Opt-in edition preference; no Room schema or upstream backup format changes. */
@Composable
internal fun rememberColorOsCourseCardMode(): NativeCourseCardMode? {
    val host = LocalContext.current.applicationContext
    val source = remember(host) { ColorOsNativeUi.source(host) } ?: return null
    val available = remember(source) {
        ColorOsNativeUi.prepare("check course card material capability") {
            source.load(MaterialEffect).getMethod("canApplyBackgroundRenderEffect").invoke(null) == true &&
                source.load(StrokeType).getField(CourseStrokePreset).get(null) != null &&
                source.load(MaterialEffect).getMethod("setViewStrokeEffect", View::class.java, source.load(StrokeType)) != null &&
                source.load(LightDrawable).getMethod("onTouchEvent", MotionEvent::class.java) != null
        } == true
    }
    if (!available) return null
    val preferences = remember(host) { host.getSharedPreferences(CourseMaterialPreference, Context.MODE_PRIVATE) }
    var enabled by remember(preferences) { mutableStateOf(preferences.getBoolean(CourseMaterialEnabled, false)) }
    var tint by remember(preferences) { mutableFloatStateOf(preferences.getFloat(CourseTintStrength, 0.5f)) }
    val blurRadius = remember(source) {
        val context = source.themedContext(host, false)
        val type = source.load(BlurType)
        val params = source.load(MaterialEffect).getMethod("getBlurParamsByType", Context::class.java, type)
            .invoke(null, context, type.getField(CourseBlurPreset).get(null))
        source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
            .getMethod("getBlurRadius", Context::class.java).invoke(params, context) as Float
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == CourseMaterialEnabled) enabled = prefs.getBoolean(key, false)
            if (key == CourseTintStrength) tint = prefs.getFloat(key, 0.5f)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return NativeCourseCardMode(enabled,
        onEnabledChange = { preferences.edit().putBoolean(CourseMaterialEnabled, it).apply() },
        tintStrength = tint,
        blurRadiusPx = blurRadius,
        onTintStrengthChange = { value, commit ->
            tint = value.coerceIn(0f, 1f)
            if (commit) preferences.edit().putFloat(CourseTintStrength, tint).apply()
        },
        onPreviewFinished = {
            tint = preferences.getFloat(CourseTintStrength, 0.5f)
        })
}

/** Only the background View receives native sampling; course text stays sharp in Compose. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ColorOsCourseCard(request: NativeCourseCardRequest, content: @Composable () -> Unit): Boolean {
    if (LocalNativeCourseCardMode.current?.enabled != true) return false
    val native = rememberNativeContext(request.dark) ?: return false
    val source = native.first
    val context = native.second
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val surface = remember(native) {
        ColorOsNativeUi.prepare("create a course card material surface") { NativeCourseSurface(context, source) }
    } ?: return false
    val outlineProvider = remember(request.shape, density, direction) {
        object : ViewOutlineProvider() {
            override fun getOutline(view: View, target: AndroidOutline) {
                when (val outline = request.shape.createOutline(Size(view.width.toFloat(), view.height.toFloat()), direction, density)) {
                    is Outline.Rectangle -> target.setRect(0, 0, view.width, view.height)
                    is Outline.Rounded -> target.setRoundRect(0, 0, view.width, view.height, outline.roundRect.topLeftCornerRadius.x)
                    is Outline.Generic -> target.setPath(outline.path.asAndroidPath())
                }
            }
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Box(request.modifier.clip(request.shape).motionEventSpy { surface.followTouch(it) }
        .then(if (request.onClick != null) Modifier.clickable(interaction, indication = null,
            role = Role.Button, onClick = request.onClick) else Modifier)) {
        if (request.renderSurface) AndroidView(factory = { surface }, modifier = Modifier.matchParentSize(), update = {
            source.updateTheme(context, request.dark)
            it.outlineProvider = outlineProvider
            it.invalidateOutline()
            it.configure(request, density, direction)
        })
        content()
    }
    DisposableEffect(surface) { onDispose { surface.release() } }
    return true
}

private class NativeCourseSurface(context: Context, private val source: ColorOsNativeUi.Source) : View(context) {
    private val effect = source.load(MaterialEffect)
    private val blurType = source.load(BlurType)
    private val strokeType = source.load(StrokeType)
    private val setBlur = effect.getMethod("setViewBlurEffect", View::class.java, blurType)
    private val setStroke = effect.getMethod("setViewStrokeEffect", View::class.java, strokeType)
    private val managerType = source.load("com.coui.appcompat.COUIMaterialStateManager")
    private val manager = managerType.getField("INSTANCE").get(null)
    private val light = source.load(LightDrawable).getConstructor().newInstance() as Drawable
    private val lightTouch: Method = light.javaClass.getMethod("onTouchEvent", MotionEvent::class.java)
    private val location = IntArray(2)
    private val tintDrawScope = CanvasDrawScope()
    private var tintDensity: Density = Density(1f)
    private var tintDirection = LayoutDirection.Ltr
    private var request: NativeCourseCardRequest? = null
    private var released = false
    private val stateListenerType = source.load("com.coui.appcompat.OnMaterialStateChangeListener")
    private val stateListener = source.listener(stateListenerType.name) { name, _ ->
        if (name == "onMaterialStateChange" && !released) applyMaterial()
        null
    }

    init {
        clipToOutline = true
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        foreground = light
        val type = source.load(LightType)
        light.javaClass.getMethod("setSpotLightType", Context::class.java, type)
            .invoke(light, context, type.getField(CourseLightPreset).get(null))
        source.setFollowLightIntensity(light)
    }

    fun configure(value: NativeCourseCardRequest, density: Density, direction: LayoutDirection) {
        if (request == value && tintDensity == density && tintDirection == direction) return
        request = value
        tintDensity = density
        tintDirection = direction
        if (isAttachedToWindow) applyMaterial()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        managerType.getMethod("addOnMaterialStateChangedListener", stateListenerType).invoke(manager, stateListener)
        applyMaterial()
    }

    override fun onDetachedFromWindow() {
        managerType.getMethod("removeOnMaterialStateChangedListener", stateListenerType).invoke(manager, stateListener)
        clearMaterial()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isAttachedToWindow) applyMaterial()
    }

    override fun draw(canvas: Canvas) {
        light.setBounds(0, 0, width, height)
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawTint = request?.onDrawTint ?: return
        // Reuse the glass module's paint after native blur and before the native foreground
        // light. This adds no second backdrop sampler or RenderEffect to the card.
        tintDrawScope.draw(tintDensity, tintDirection, androidx.compose.ui.graphics.Canvas(canvas),
            Size(width.toFloat(), height.toFloat()), drawTint)
    }

    private fun applyMaterial() {
        val value = request ?: return
        if (width <= 0 || height <= 0) return
        val blur = value.materialVisible && managerType.getMethod("isBlurEffectEnable").invoke(manager) == true
        val stroke = value.materialVisible && managerType.getMethod("isStrokeEffectEnable").invoke(manager) == true
        val glow = value.materialVisible && managerType.getMethod("isSpotLightEffectEnable").invoke(manager) == true
        // Keep the content-glass blur and course gradient; use the Dock's native edge/light
        // presets for stronger top/bottom highlights, without changing the card's outline.
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setBlur.invoke(null, this, blurType.getField(if (blur) CourseBlurPreset else "TYPE_NO_EFFECT").get(null))
        setStroke.invoke(null, this, strokeType.getField(
            if (stroke) CourseStrokePreset else "TYPE_NO_EFFECT").get(null))
        light.javaClass.getMethod("setEnabled", Boolean::class.javaPrimitiveType).invoke(light, glow)
        invalidate()
    }

    fun followTouch(event: MotionEvent) {
        if (!isAttachedToWindow || released || request?.materialVisible != true) return
        getLocationOnScreen(location)
        val local = MotionEvent.obtain(event)
        try {
            local.setLocation(event.rawX - location[0], event.rawY - location[1])
            lightTouch.invoke(light, local)
        } finally { local.recycle() }
    }

    private fun clearBlur() {
        val type = source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType")
        effect.getMethod("setViewBlurEffect", View::class.java, type).invoke(null, this, type.getField("TYPE_NO_EFFECT").get(null))
    }

    private fun clearMaterial() {
        clearBlur()
        setStroke.invoke(null, this, strokeType.getField("TYPE_NO_EFFECT").get(null))
        light.javaClass.getMethod("cancel").invoke(light)
    }

    fun release() { released = true; clearMaterial() }
}
