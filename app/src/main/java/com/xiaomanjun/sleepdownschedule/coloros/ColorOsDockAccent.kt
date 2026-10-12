package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.xiaomanjun.sleepdownschedule.core.performance.HomeWallpaperImages
import com.xiaomanjun.sleepdownschedule.glass.ui.DefaultCourseCardPalette

/** Only home dock consumers use this accent; toolbar/search spotlights keep their native color. */
internal val LocalColorOsDockAccent = compositionLocalOf<Int?> { null }

internal fun dockAccent(source: ColorOsNativeUi.Source, context: Context, wallpaperAccent: Int?): Int =
    wallpaperAccent ?: source.color(context, "couiColorContainerTheme").defaultColor

@Composable
internal fun rememberColorOsDockAccent(images: HomeWallpaperImages, background: Int): Int? {
    val host = LocalContext.current
    val source = remember(host) { ColorOsNativeUi.source(host) } ?: return null
    val native = rememberNativeContext(dark = false) ?: return null
    return remember(images.source, images.representativeColors, background, native) {
        val fallback = dockAccent(source, native.second, null)
        if (images.source == null || images.representativeColors == DefaultCourseCardPalette) return@remember fallback
        val seed = images.representativeColors.firstOrNull { color ->
            val hsv = FloatArray(3)
            Color.colorToHSV(color.toInt(), hsv)
            hsv[1] >= 0.16f && hsv[2] >= 0.22f
        } ?: return@remember colorOsContrastingAccent(fallback, background)
        val hsv = FloatArray(3)
        Color.colorToHSV(seed.toInt(), hsv)
        // Keep the wallpaper hue while leaving enough contrast for white plus and small labels.
        hsv[1] = hsv[1].coerceIn(0.50f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.60f, 0.80f)
        colorOsContrastingAccent(Color.HSVToColor(hsv), background)
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

/** A brighter selected label must not make the FAB's fixed white plus disappear. */
internal fun colorOsAccentForWhiteIcon(accent: Int): Int {
    if (ColorUtils.calculateContrast(Color.WHITE, accent) >= 3.0) return accent
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(accent, hsl)
    val original = hsl[2]
    var low = 0f
    var high = original
    repeat(18) {
        hsl[2] = (low + high) / 2f
        if (ColorUtils.calculateContrast(Color.WHITE, ColorUtils.HSLToColor(hsl)) >= 3.0) low = hsl[2]
        else high = hsl[2]
    }
    hsl[2] = low
    return ColorUtils.HSLToColor(hsl)
}
