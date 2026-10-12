package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.performance.effectiveAppMaterialPolicy

private const val AppBarBlurHelper = "com.coui.appcompat.toolbar.AppBarBlurHelper"

/** Keep the native sampler in the page's original chrome position, inside its scene capture. */
@Composable
internal fun ColorOsTopGradientBlur(modifier: Modifier, dark: Boolean): Boolean {
    if (effectiveAppMaterialPolicy().simplifiedProgressiveBlur) return false
    val native = rememberNativeContext(dark) ?: return false
    var failed by remember(native) { mutableStateOf(false) }
    if (failed) return false
    val surface = remember(native) {
        ColorOsNativeUi.prepare("create native top gradient blur") {
            val effect = native.first.load("com.coui.appcompat.COUIMaterialEffect")
            if (effect.getMethod("canApplyBackgroundRenderEffect").invoke(null) != true) null
            else NativeTopGradientSurface(native.second, native.first) { failed = true }
        }
    } ?: return false
    AndroidView(factory = { surface }, modifier = modifier, update = {
        if (native.first.updateTheme(native.second, dark) && it.isAttachedToWindow) it.applyMaterial()
    })
    DisposableEffect(surface) { onDispose { surface.release() } }
    return true
}

private class NativeTopGradientSurface(
    context: Context,
    source: ColorOsNativeUi.Source,
    private val onFailure: () -> Unit
) : View(context) {
    private val helper = source.load(AppBarBlurHelper)
    private val setup = helper.getMethod("setUpGradientBlurBackground", View::class.java)
    private val clear = helper.getMethod("clearGradientBlur", View::class.java)
    private var released = false

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyMaterial()
    }

    fun applyMaterial() {
        if (released) return
        // This COUI entry owns the native variable-radius effect, gradient tint, system
        // material-state listener and low-animation policy; do not stack the old sampler.
        val applied = ColorOsNativeUi.prepare("apply native top gradient blur") {
            setup.invoke(null, this)
            true
        } == true
        if (!applied) onFailure()
    }

    override fun onDetachedFromWindow() {
        clearMaterial()
        super.onDetachedFromWindow()
    }

    private fun clearMaterial() {
        ColorOsNativeUi.prepare("clear native top gradient blur") { clear.invoke(null, this) }
    }

    fun release() {
        released = true
        clearMaterial()
    }
}
