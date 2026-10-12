package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BlendMode
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCourseCardMode
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCourseCardRequest
import java.lang.reflect.Method

private const val CourseMaterialPreference = "coloros_course_material"
private const val CourseMaterialEnabled = "enabled"
private const val MaterialEffect = "com.coui.appcompat.COUIMaterialEffect"
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
                source.load(StrokeType).getField("TYPE_CONTENT_CAPSULE_1").get(null) != null &&
                source.load(LightDrawable).getMethod("onTouchEvent", MotionEvent::class.java) != null
        } == true
    }
    if (!available) return null
    val preferences = remember(host) { host.getSharedPreferences(CourseMaterialPreference, Context.MODE_PRIVATE) }
    var enabled by remember(preferences) { mutableStateOf(preferences.getBoolean(CourseMaterialEnabled, false)) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == CourseMaterialEnabled) enabled = prefs.getBoolean(key, false)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return NativeCourseCardMode(enabled) { preferences.edit().putBoolean(CourseMaterialEnabled, it).apply() }
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
            it.configure(request)
        })
        content()
    }
    DisposableEffect(surface) { onDispose { surface.release() } }
    return true
}

private class NativeCourseSurface(context: Context, private val source: ColorOsNativeUi.Source) : View(context) {
    private val effect = source.load(MaterialEffect)
    private val blurParamsType = source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectParams")
    private val baseParamsType = source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    private val strokeType = source.load(StrokeType)
    private val setBlur = effect.getMethod("setViewBlurEffect", View::class.java, baseParamsType)
    private val setStroke = effect.getMethod("setViewStrokeEffect", View::class.java, strokeType)
    private val managerType = source.load("com.coui.appcompat.COUIMaterialStateManager")
    private val manager = managerType.getField("INSTANCE").get(null)
    private val light = source.load(LightDrawable).getConstructor().newInstance() as Drawable
    private val lightTouch: Method = light.javaClass.getMethod("onTouchEvent", MotionEvent::class.java)
    private val location = IntArray(2)
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
            .invoke(light, context, type.getField("TYPE_TRANSLUCENT_LARGE_1").get(null))
    }

    fun configure(value: NativeCourseCardRequest) {
        if (request == value) return
        request = value
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

    override fun draw(canvas: Canvas) {
        light.setBounds(0, 0, width, height)
        super.draw(canvas)
    }

    private fun applyMaterial() {
        val value = request ?: return
        val blur = value.materialVisible && managerType.getMethod("isBlurEffectEnable").invoke(manager) == true
        val stroke = value.materialVisible && managerType.getMethod("isStrokeEffectEnable").invoke(manager) == true
        val glow = value.materialVisible && managerType.getMethod("isSpotLightEffectEnable").invoke(manager) == true
        if (blur) {
            // Native two-layer material retains course tint, using the system background sampler.
            val tint = value.color.copy(alpha = value.tintAlpha).toArgb()
            val params = blurParamsType.getConstructor(Float::class.javaPrimitiveType, String::class.java,
                BlendMode::class.java, String::class.java, BlendMode::class.java)
                .newInstance(value.blurRadiusPx.coerceAtLeast(1f), "#00000000", BlendMode.SRC_OVER,
                    "#" + Integer.toHexString(tint).padStart(8, '0'), BlendMode.SRC_OVER)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setBlur.invoke(null, this, params)
        } else {
            clearBlur()
            setBackgroundColor(value.color.copy(alpha = value.tintAlpha).toArgb())
        }
        setStroke.invoke(null, this, strokeType.getField(if (stroke) "TYPE_CONTENT_CAPSULE_1" else "TYPE_NO_EFFECT").get(null))
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
