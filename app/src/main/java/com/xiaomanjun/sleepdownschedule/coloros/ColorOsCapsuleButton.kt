package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeCapsuleButtonRequest

/** COUI supplies the material and motion; Compose draws live content using that same motion. */
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
    val motion = remember(button) { ColorOsCapsuleMotion() }
    val motionBinding = remember(button) {
        ColorOsNativeUi.prepare("bind native capsule content motion") {
            ColorOsCapsuleMotionBinding(native.first, button, motion)
        }
    } ?: return false
    NativeCapsuleButtonLayout(request, button, motion, update = {
        val changed = it.isEnabled != request.enabled || appearance.accent != accent
        it.isEnabled = request.enabled
        if (primary && changed) native.first.applyCapsuleAccent(it, checkNotNull(accent))
        appearance.accent = accent
    }, content = content)
    DisposableEffect(button, motionBinding) {
        onDispose {
            motionBinding.dispose()
            button.setOnClickListener(null)
            button.setOnTouchListener(null)
        }
    }
    return true
}

/** Normal Compose drawing keeps text, icons and loading content visible and invalidatable. */
@Composable
internal fun NativeCapsuleButtonLayout(
    request: NativeCapsuleButtonRequest,
    button: TextView,
    motion: ColorOsCapsuleMotion,
    update: (TextView) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
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
        AndroidView(factory = { button }, modifier = Modifier.matchParentSize(), update = update)
        // Read motion only in the layer block: native animation updates transform properties,
        // without recomposing or recording the content through a foreign ViewOverlay canvas.
        Row(Modifier.graphicsLayer { motion.applyTo(this) }
            .height(request.height).padding(request.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

private class CapsuleAppearance { var accent: Int? = null }

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
