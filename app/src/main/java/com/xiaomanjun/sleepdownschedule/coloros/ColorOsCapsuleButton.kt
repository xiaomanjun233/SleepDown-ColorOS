package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCapsuleButtonRequest
import com.xiaomanjun.sleepdownschedule.glass.withRecordingDensity

/** COUI draws both material and Compose content inside its native touch-animation transform. */
@Composable
internal fun ColorOsCapsuleButton(request: NativeCapsuleButtonRequest, content: @Composable RowScope.() -> Unit): Boolean {
    val native = rememberNativeContext(request.dark) ?: return false
    val latest = rememberUpdatedState(request)
    val primary = request.primaryColor.isSpecified
    val button = remember(native, request.dark, primary) {
        ColorOsNativeUi.prepare("create a capsule button") {
            native.first.updateTheme(native.second, request.dark)
            val view = native.first.load("com.coui.appcompat.button.COUIButton")
                .getConstructor(Context::class.java).newInstance(native.second) as TextView
            native.first.configureCapsuleButton(view, primary)
            view.apply {
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setPadding(0, 0, 0, 0)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                isFocusable = false
                setOnClickListener { if (latest.value.enabled) latest.value.onClick() }
                var rejected = false
                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        rejected = !latest.value.acceptsGesture(Size(width.toFloat(), height.toFloat()), Offset(event.x, event.y))
                    }
                    rejected
                }
            }
            native.first.bindFollowLightIntensity(view, afterMaterialApplied = {
                if (primary) native.first.applyCapsuleAccent(view, latest.value.primaryColor.toArgb())
            })
            view
        }
    } ?: return false
    val accent = if (primary) request.primaryColor.toArgb() else null
    val appearance = remember(button) { CapsuleAppearance() }
    val density = LocalDensity.current
    val contentLayer = rememberGraphicsLayer()
    val nativeContent = remember(button, contentLayer) { CapsuleContentDrawable(contentLayer) }
    DisposableEffect(button, nativeContent) {
        button.overlay.add(nativeContent)
        onDispose { button.overlay.remove(nativeContent) }
    }
    Box(request.modifier.height(request.height)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (!request.enabled) disabled()
            onClick { if (request.enabled) { button.performClick(); true } else false }
        }
        .onKeyEvent {
            val activate = it.key == Key.Enter || it.key == Key.NumPadEnter || it.key == Key.Spacebar || it.key == Key.DirectionCenter
            if (request.enabled && activate) {
                if (it.type == KeyEventType.KeyUp) button.performClick()
                true
            } else false
        }.focusable(request.enabled), propagateMinConstraints = true) {
        // Retain Compose measurement, semantics and arbitrary icon/loading content, but draw it
        // through COUIButton's overlay. Its native animator scales/translates the whole View.
        // The outer layer caches this recording so native animation invalidation does not
        // re-record the label or feed an invalidate/record loop back into Compose.
        Row(Modifier.graphicsLayer().drawWithContent {
            contentLayer.record {
                withRecordingDensity(density) { this@drawWithContent.drawContent() }
            }
            nativeContent.onContentRecorded(density, layoutDirection, size)
        }.height(request.height).padding(request.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically, content = content)
        AndroidView(factory = { button }, modifier = Modifier.matchParentSize(), update = {
            val changed = it.isEnabled != request.enabled || appearance.accent != accent
            it.isEnabled = request.enabled
            if (primary && changed) native.first.applyCapsuleAccent(it, checkNotNull(accent))
            appearance.accent = accent
        })
    }
    DisposableEffect(button) {
        onDispose { button.setOnClickListener(null); button.setOnTouchListener(null) }
    }
    return true
}

private class CapsuleAppearance { var accent: Int? = null }

/** A display list, not a bitmap snapshot; the COUI View supplies the only motion transform. */
private class CapsuleContentDrawable(private val layer: GraphicsLayer) : Drawable() {
    private val scope = CanvasDrawScope()
    private var density: Density = Density(1f)
    private var direction = LayoutDirection.Ltr
    private var size = Size.Zero

    fun onContentRecorded(density: Density, direction: LayoutDirection, size: Size) {
        this.density = density
        this.direction = direction
        this.size = size
        setBounds(0, 0, size.width.toInt(), size.height.toInt())
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (size == Size.Zero || layer.isReleased) return
        scope.draw(density, direction, androidx.compose.ui.graphics.Canvas(canvas), size) {
            drawLayer(layer)
        }
    }

    override fun setAlpha(alpha: Int) { layer.alpha = alpha / 255f; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

internal fun ColorOsNativeUi.Source.configureCapsuleButton(view: TextView, primary: Boolean) {
    val type = load("com.coui.appcompat.button.COUIButton")
    val buttonType = load("${type.name}\$ButtonType")
    val kind = buttonType.getField(if (primary) "PRIMARY" else "SECONDARY").get(null)
    val state = load("${type.name}\$MaterialEffectState")
    // The one-argument setter resets the type to UNKNOWN; always supply both values.
    type.getMethod("setUseMaterialEffectAttr", state, buttonType)
        .invoke(view, state.getField("FOLLOW_SYSTEM").get(null), kind)
    type.getMethod("setIsDrawableMaterialEffect", Boolean::class.javaPrimitiveType).invoke(view, false)
    type.getMethod("setDrawableRadius", Int::class.javaPrimitiveType).invoke(view, -1)
    type.getMethod("setButtonType", buttonType).invoke(view, kind)
    type.getMethod("setAnimEnable", Boolean::class.javaPrimitiveType).invoke(view, true)
}

/** Keep COUI's primary preset (including disabled alpha); change only the application accent. */
internal fun ColorOsNativeUi.Source.applyCapsuleAccent(view: TextView, accent: Int) {
    val type = load("com.coui.appcompat.button.COUIButton")
    type.getMethod("setDrawableColor", Int::class.javaPrimitiveType).invoke(view, accent or (0xFF shl 24))
    if (!view.isAttachedToWindow || !blurEnabled()) { view.invalidate(); return }
    val theme = load("com.coui.appcompat.effect.COUIMaterialThemeBlurEffect")
    val companion = theme.getField("Companion").get(null)
    val presetType = load("${theme.name}\$ThemeBlurEffectType")
    val preset = companion.javaClass.getMethod("getBlurEffectParamsByType", Context::class.java, presetType)
        .invoke(companion, view.context, presetType.getField(if (view.isEnabled)
            "TYPE_CONTENT_PRIMARY_BUTTON" else "TYPE_CONTENT_DISABLED_PRIMARY_BUTTON").get(null))
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val original = base.getMethod("getColorLayer1", Context::class.java).invoke(preset, view.context) as Int
    val colored = (accent and 0x00FFFFFF) or (AndroidColor.alpha(original) shl 24)
    fun hex(color: Int) = "#" + Integer.toHexString(color).padStart(8, '0')
    val params = load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectParams")
        .getConstructor(Float::class.javaPrimitiveType, String::class.java, BlendMode::class.java,
            String::class.java, BlendMode::class.java)
        .newInstance(base.getMethod("getBlurRadius", Context::class.java).invoke(preset, view.context), hex(colored),
            base.getMethod("getBlendModeLayer1").invoke(preset),
            hex(base.getMethod("getColorLayer2", Context::class.java).invoke(preset, view.context) as Int),
            base.getMethod("getBlendModeLayer2").invoke(preset))
    load("com.coui.appcompat.COUIMaterialEffect").getMethod("setViewBlurEffect", View::class.java, base)
        .invoke(null, view, params)
    view.invalidate()
}
