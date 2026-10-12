package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.xiaomanjun.sleepdownschedule.core.performance.HomeWallpaperImages
import com.xiaomanjun.sleepdownschedule.glass.ui.DefaultCourseCardPalette

/** Only home dock consumers use this accent; toolbar/search spotlights keep their native color. */
internal val LocalColorOsDockAccent = compositionLocalOf<Int?> { null }
internal val LocalColorOsButtonAccent = compositionLocalOf<Int?> { null }

@Immutable
internal data class ColorOsHomeAccents(val button: Int, val dock: Int)

internal fun dockAccent(source: ColorOsNativeUi.Source, context: Context, wallpaperAccent: Int?): Int =
    wallpaperAccent ?: source.color(context, "couiColorContainerTheme").defaultColor

@Composable
internal fun rememberColorOsHomeAccents(images: HomeWallpaperImages, background: Int): ColorOsHomeAccents? {
    val host = LocalContext.current
    val source = remember(host) { ColorOsNativeUi.source(host) } ?: return null
    val native = rememberNativeContext(dark = false) ?: return null
    return remember(images.source, images.representativeColors, background, native) {
        val fallback = dockAccent(source, native.second, null)
        if (images.source == null || images.representativeColors == DefaultCourseCardPalette) {
            return@remember ColorOsHomeAccents(fallback, fallback)
        }
        val seed = images.representativeColors.firstOrNull { color ->
            val hsv = FloatArray(3)
            Color.colorToHSV(color.toInt(), hsv)
            hsv[1] >= 0.16f && hsv[2] >= 0.22f
        } ?: return@remember ColorOsHomeAccents(fallback, colorOsContrastingAccent(fallback, background))
        val hsv = FloatArray(3)
        Color.colorToHSV(seed.toInt(), hsv)
        // Keep the wallpaper hue while leaving enough contrast for white plus and small labels.
        hsv[1] = hsv[1].coerceIn(0.50f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.60f, 0.80f)
        val button = Color.HSVToColor(hsv)
        ColorOsHomeAccents(button, colorOsContrastingAccent(button, background))
    }
}

/** Preserve wallpaper hue; adjust lightness against the visible, brightness-adjusted backdrop. */
internal fun colorOsContrastingAccent(seed: Int, background: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val brighten = ColorUtils.calculateLuminance(background) < 0.18
    val preferred = if (brighten) maxOf(hsl[2], 0.66f) else minOf(hsl[2], 0.42f)
    hsl[2] = preferred
    val candidate = ColorUtils.HSLToColor(hsl)
    if (ColorUtils.calculateContrast(candidate, background) >= 4.5) return candidate
    var low = 0f
    var high = 1f
    repeat(18) {
        val amount = (low + high) / 2f
        hsl[2] = preferred + ((if (brighten) 1f else 0f) - preferred) * amount
        if (ColorUtils.calculateContrast(ColorUtils.HSLToColor(hsl), background) >= 4.5) high = amount
        else low = amount
    }
    hsl[2] = preferred + ((if (brighten) 1f else 0f) - preferred) * high
    return ColorUtils.HSLToColor(hsl)
}
