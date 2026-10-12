package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.xiaomanjun.sleepdownschedule.core.performance.HomeWallpaperImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val wallpaperAccent by produceState<Int?>(null, images.source) {
        value = withContext(Dispatchers.Default) { colorOsWallpaperAccent(images.source) }
    }
    return remember(wallpaperAccent, images.source, background, native) {
        val fallback = dockAccent(source, native.second, null)
        if (images.source == null) return@remember ColorOsHomeAccents(fallback, fallback)
        val button = wallpaperAccent ?: fallback
        ColorOsHomeAccents(button, colorOsContrastingAccent(button, background))
    }
}

/** Course palettes discard dark swatches; chrome must extract its own hue before brightening it. */
internal fun colorOsWallpaperAccent(bitmap: Bitmap?): Int? {
    if (bitmap == null || bitmap.isRecycled) return null
    val hsv = FloatArray(3)
    val seed = Palette.from(bitmap).clearFilters().maximumColorCount(12).resizeBitmapArea(96 * 96)
        .generate().swatches.sortedByDescending { it.population }.firstOrNull {
            Color.colorToHSV(it.rgb, hsv)
            hsv[1] >= 0.16f && hsv[2] >= 0.04f
        } ?: return null
    Color.colorToHSV(seed.rgb, hsv)
    hsv[1] = hsv[1].coerceIn(0.50f, 0.85f)
    hsv[2] = hsv[2].coerceIn(0.60f, 0.80f)
    return Color.HSVToColor(hsv)
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
