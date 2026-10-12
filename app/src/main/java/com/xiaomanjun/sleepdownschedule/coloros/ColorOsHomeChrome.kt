package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.BlendMode
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.DockAlignment
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.app.ui.dockImeCompensationPx
import com.xiaomanjun.sleepdownschedule.core.performance.DockEntranceContainer
import com.xiaomanjun.sleepdownschedule.core.performance.StartupPhase
import com.xiaomanjun.sleepdownschedule.feature.agent.homeCountdownShockwave

/** Weather uses COUI's neutral/transparent button material, independent of its sky colors. */
@Immutable
internal data class ColorOsHomeChromeStyle(
    val dark: Boolean,
    val weatherMaterial: Boolean,
    val foreground: Int?
)

internal val LocalColorOsHomeChromeStyle = compositionLocalOf<ColorOsHomeChromeStyle?> { null }

/** The morph target and expanded menu share one neutral tint. The FAB keeps its accent. */
internal fun colorOsHomeNeutralTint(dark: Boolean) =
    androidx.compose.ui.graphics.Color(if (dark) 0xFF050505 else 0xFFFFFFFF)
        .copy(alpha = if (dark) 0.40f else 0.28f)

internal fun ColorOsNativeUi.Source.weatherButtonBlurParams(context: Context, radiusScale: Float = 1f): Any {
    val effect = load("com.coui.appcompat.COUIMaterialEffect")
    val type = load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType")
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val preset = effect.getMethod("getBlurParamsByType", Context::class.java, type)
        .invoke(null, context, type.getField("TYPE_CONTENT_TRANSPARENT_BUTTON").get(null))
    if (radiusScale == 1f) return preset
    fun color(name: String): String = "#" + Integer.toHexString(
        base.getMethod(name, Context::class.java).invoke(preset, context) as Int).padStart(8, '0')
    val radius = base.getMethod("getBlurRadius", Context::class.java).invoke(preset, context) as Float
    return load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectParams")
        .getConstructor(Float::class.javaPrimitiveType, String::class.java, BlendMode::class.java,
            String::class.java, BlendMode::class.java)
        .newInstance(radius * radiusScale, color("getColorLayer1"),
            base.getMethod("getBlendModeLayer1").invoke(preset), color("getColorLayer2"),
            base.getMethod("getBlendModeLayer2").invoke(preset))
}

internal fun ColorOsNativeUi.Source.homeFabBlurParams(context: Context, fabPreset: Any): Any {
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val radius = base.getMethod("getBlurRadius", Context::class.java).invoke(fabPreset, context) as Float
    val themeParams = load("com.coui.appcompat.effect.COUIMaterialThemeBlurEffect\$Companion\$ThemeBlurEffectParams")
    return themeParams.getConstructor(Float::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType, BlendMode::class.java, Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType, BlendMode::class.java, Int::class.javaPrimitiveType)
        .newInstance(radius * 0.85f, base.getMethod("getColorLayer1", Context::class.java).invoke(fabPreset, context),
            false, base.getMethod("getBlendModeLayer1").invoke(fabPreset),
            base.getMethod("getColorLayer2", Context::class.java).invoke(fabPreset, context), false,
            base.getMethod("getBlendModeLayer2").invoke(fabPreset), 255)
}

/** Native-only input scope: no schedule config, backdrop, overlay controller or animation value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativeHomeDockChrome(
    visible: Boolean, parallel: Boolean, selectedIndex: Int, dark: Boolean,
    alignment: DockAlignment, buttonHidden: Boolean,
    onSelect: (Int) -> Unit, onHome: () -> Unit, onOpenMenu: (Rect, GraphicsLayer) -> Unit,
    fallback: State<@Composable () -> Unit>,
    phase: StartupPhase,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    // Aggregate composition commits only; no user data or per-frame state is read here.
    SideEffect { Log.i("SleepDownCOUI", "Native dock commit: parallel=$parallel, selected=$selectedIndex, dark=$dark, hidden=$buttonHidden") }
    val restingModifier = modifier.homeCountdownShockwave(0.85f)
    DockEntranceContainer(phase, restingModifier) {
    if (parallel) {
        if (!ColorOsParallelHomeDock(selectedIndex, dark, buttonHidden, Modifier, onSelect, onOpenMenu)) NativeDockFallback(fallback)
    } else {
        val density = LocalDensity.current
        val systemBottom = WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom).getBottom(density)
        val bottom = with(density) { systemBottom.toDp() } + 8.dp
        val compensation = dockImeCompensationPx(WindowInsets.ime.getBottom(density), systemBottom)
        val position = when (alignment) {
            DockAlignment.LEFT -> Alignment.BottomStart
            DockAlignment.CENTER -> Alignment.BottomCenter
            DockAlignment.RIGHT -> Alignment.BottomEnd
        }
        val click = remember(onSelect, onHome) { { index: Int -> if (index == 0) onHome() else onSelect(2) } }
        val tabs = remember { listOf(R.drawable.ic_week_view to "课程", R.drawable.ic_settings to "设置") }
        Box(Modifier.fillMaxSize(), contentAlignment = position) {
            Box(Modifier.padding(start = 18.dp, end = 18.dp, bottom = bottom)
                .graphicsLayer { translationY = compensation.toFloat() }) {
                if (!ColorOsHomeDock(tabs, if (selectedIndex == 2) 1 else 0, dark,
                        Modifier.width(colorOsOrdinaryDockWidth()), click)) NativeDockFallback(fallback)
            }
        }
    }
    }
}

@Composable
private fun NativeDockFallback(content: State<@Composable () -> Unit>) { content.value() }
