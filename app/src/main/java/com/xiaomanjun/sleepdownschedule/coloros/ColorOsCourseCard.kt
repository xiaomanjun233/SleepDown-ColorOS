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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMaterialSurfaceRequest
import java.lang.reflect.Method

private const val CourseMaterialPreference = "coloros_course_material"
private const val CourseMaterialEnabled = "enabled"
private const val CourseTintStrength = "tint_strength"
private const val CourseBlurScale = "blur_radius_scale"
private const val MaterialEffect = "com.coui.appcompat.COUIMaterialEffect"
private const val BlurType = "com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType"
private const val CourseBlurPreset = "TYPE_CONTENT_TRANSPARENT_BUTTON"
// COUIButton's transparent content preset; its corner params are installed before the stroke.
private const val CourseStrokePreset = "TYPE_CONTENT_CAPSULE_3"
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
    val enabled = remember(preferences) { mutableStateOf(preferences.getBoolean(CourseMaterialEnabled, false)) }
    val tint = remember(preferences) { mutableFloatStateOf(preferences.getFloat(CourseTintStrength, 0.5f).coerceIn(0f, 1f)) }
    val blurScale = remember(preferences) { mutableFloatStateOf(preferences.getFloat(CourseBlurScale, 1f).coerceIn(0f, 2f)) }
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
            if (key == CourseMaterialEnabled) enabled.value = prefs.getBoolean(key, false)
            if (key == CourseTintStrength) tint.floatValue = prefs.getFloat(key, 0.5f).coerceIn(0f, 1f)
            if (key == CourseBlurScale) blurScale.floatValue = prefs.getFloat(key, 1f).coerceIn(0f, 2f)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return remember(preferences, blurRadius) { NativeCourseCardMode(enabled,
        onEnabledChange = { preferences.edit().putBoolean(CourseMaterialEnabled, it).apply() },
        tintState = tint,
        blurScaleState = blurScale,
        defaultBlurRadiusPx = blurRadius,
        onTintStrengthChange = { value, commit ->
            tint.floatValue = value.coerceIn(0f, 1f)
            if (commit) preferences.edit().putFloat(CourseTintStrength, tint.floatValue).apply()
        },
        onBlurRadiusScaleChange = { value, commit ->
            blurScale.floatValue = value.coerceIn(0f, 2f)
            if (commit) preferences.edit().putFloat(CourseBlurScale, blurScale.floatValue).apply()
        },
        onPreviewFinished = {
            tint.floatValue = preferences.getFloat(CourseTintStrength, 0.5f).coerceIn(0f, 1f)
            blurScale.floatValue = preferences.getFloat(CourseBlurScale, 1f).coerceIn(0f, 2f)
        }) }
}

/** Only the background View receives native sampling; course text stays sharp in Compose. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ColorOsCourseCard(request: NativeCourseCardRequest, content: @Composable () -> Unit): Boolean {
    val mode = LocalNativeCourseCardMode.current ?: return false
    if (!mode.enabled) return false
    return ColorOsMaterialSurface(request.copy(blurRadiusScale = mode.blurRadiusScale), content)
}

/** Shared native backdrop for cards, table headers and assistant shells, with no input interception. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ColorOsMaterialSurface(request: NativeMaterialSurfaceRequest, content: @Composable () -> Unit): Boolean {
    val native = rememberNativeContext(request.dark) ?: return false
    val source = native.first
    val context = native.second
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val surface = remember(native) {
        ColorOsNativeUi.prepare("create a material surface") { NativeMaterialSurface(context, source) }
    } ?: return false
    val latest = rememberUpdatedState(request)
    val interaction = remember { MutableInteractionSource() }
    Box(request.modifier.graphicsLayer {
        val value = latest.value
        shape = value.shapeProvider?.invoke() ?: value.shape
        clip = true
    }.motionEventSpy { surface.followTouch(it) }
        .then(if (request.onClick != null) Modifier.clickable(interaction, indication = null,
            role = Role.Button, onClick = request.onClick) else Modifier)) {
        if (request.renderSurface) AndroidView(factory = { surface }, modifier = Modifier.matchParentSize()
            .graphicsLayer { alpha = latest.value.surfaceAlpha() }
            .drawWithContent {
                // Morph geometry is read in drawing, never used as a composition key.
                val value = latest.value
                surface.updateOutline(value.shapeProvider?.invoke() ?: value.shape, density, direction)
                drawContent()
            }, update = {
            source.updateTheme(context, request.dark)
            it.updateOutline(request.shape, density, direction)
            it.configure(request, density, direction)
        })
        CompositionLocalProvider(
            com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMaterialSurfaceActive provides true,
            content = content
        )
    }
    DisposableEffect(surface) { onDispose { surface.release() } }
    return true
}

private class NativeMaterialSurface(context: Context, private val source: ColorOsNativeUi.Source) : View(context) {
    private val effect = source.load(MaterialEffect)
    private val blurType = source.load(BlurType)
    private val strokeType = source.load(StrokeType)
    private val setBlur = effect.getMethod("setViewBlurEffect", View::class.java, blurType)
    private val setStroke = effect.getMethod("setViewStrokeEffect", View::class.java, strokeType)
    private val cornerType = source.load("com.oplus.view.material.OplusMaterialCornerParams")
    private val setCorners = effect.getMethod("setCornerParams", View::class.java, cornerType)
    private var appliedCorners: ColorOsMaterialCorners? = null
    private val managerType = source.load("com.coui.appcompat.COUIMaterialStateManager")
    private val manager = managerType.getField("INSTANCE").get(null)
    private val light = source.load(LightDrawable).getConstructor().newInstance() as Drawable
    private val lightTouch: Method = light.javaClass.getMethod("onTouchEvent", MotionEvent::class.java)
    private val location = IntArray(2)
    private val tintDrawScope = CanvasDrawScope()
    private var tintDensity: Density = Density(1f)
    private var tintDirection = LayoutDirection.Ltr
    private var request: NativeMaterialSurfaceRequest? = null
    private var surfaceShape: Shape? = null
    private var released = false
    private val stateListenerType = source.load("com.coui.appcompat.OnMaterialStateChangeListener")
    private val stateListener = source.listener(stateListenerType.name) { name, _ ->
        if (name == "onMaterialStateChange" && !released) applyMaterial()
        null
    }

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, target: AndroidOutline) {
                when (val outline = surfaceShape?.createOutline(
                    Size(width.toFloat(), height.toFloat()), tintDirection, tintDensity)) {
                    is Outline.Rectangle -> target.setRect(0, 0, width, height)
                    is Outline.Rounded -> {
                        val rect = outline.roundRect
                        val corner = rect.topLeftCornerRadius
                        if (corner.x == corner.y && corner == rect.topRightCornerRadius &&
                            corner == rect.bottomLeftCornerRadius && corner == rect.bottomRightCornerRadius)
                            target.setRoundRect(rect.left.toInt(), rect.top.toInt(),
                            rect.right.toInt(), rect.bottom.toInt(), rect.topLeftCornerRadius.x)
                        else target.setPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(rect) }.asAndroidPath())
                    }
                    is Outline.Generic -> target.setPath(outline.path.asAndroidPath())
                    null -> target.setEmpty()
                }
            }
        }
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        foreground = light
        val type = source.load(LightType)
        light.javaClass.getMethod("setSpotLightType", Context::class.java, type)
            .invoke(light, context, type.getField(CourseLightPreset).get(null))
        source.setFollowLightIntensity(light)
    }

    fun updateOutline(shape: Shape, density: Density, direction: LayoutDirection) {
        if (surfaceShape == shape && tintDensity == density && tintDirection == direction) return
        surfaceShape = shape
        tintDensity = density
        tintDirection = direction
        invalidateOutline()
        if (isAttachedToWindow) updateMaterialCorners()
    }

    private fun updateMaterialCorners() {
        val shape = surfaceShape ?: return
        val corners = colorOsMaterialCorners(shape, Size(width.toFloat(), height.toFloat()), tintDensity, tintDirection)
        if (corners == appliedCorners) return
        val params = corners?.let {
            cornerType.getConstructor(Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                .newInstance(it.radius, it.weight)
        }
        setCorners.invoke(null, this, params)
        appliedCorners = corners
    }

    fun configure(value: NativeMaterialSurfaceRequest, density: Density, direction: LayoutDirection) {
        if (request == value && tintDensity == density && tintDirection == direction) return
        val previous = request
        val materialChanged = previous == null || previous.dark != value.dark ||
            previous.materialVisible != value.materialVisible || previous.edgeEffects != value.edgeEffects ||
            previous.blurRadiusScale != value.blurRadiusScale
        request = value
        tintDensity = density
        tintDirection = direction
        // Tint/content updates only need a redraw. Reinstalling COUI effects for every slider
        // sample resets the native light/material animation even though its parameters match.
        if (isAttachedToWindow && materialChanged) applyMaterial() else invalidate()
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
        val blur = value.materialVisible && value.blurRadiusScale > 0f &&
            managerType.getMethod("isBlurEffectEnable").invoke(manager) == true
        val stroke = value.materialVisible && value.edgeEffects && managerType.getMethod("isStrokeEffectEnable").invoke(manager) == true
        val glow = value.materialVisible && value.edgeEffects && managerType.getMethod("isSpotLightEffectEnable").invoke(manager) == true
        // Match COUIButton's native order: geometry first, then the system material presets.
        updateMaterialCorners()
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        if (blur && value.blurRadiusScale != 1f) {
            effect.getMethod("setViewBlurEffect", View::class.java,
                source.load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams"))
                .invoke(null, this, source.weatherButtonBlurParams(context, value.blurRadiusScale))
        } else {
            setBlur.invoke(null, this, blurType.getField(if (blur) CourseBlurPreset else "TYPE_NO_EFFECT").get(null))
        }
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
        setCorners.invoke(null, this, null)
        appliedCorners = null
        light.javaClass.getMethod("cancel").invoke(light)
    }

    fun release() { released = true; clearMaterial() }
}
