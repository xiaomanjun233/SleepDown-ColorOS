package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xiaomanjun.sleepdownschedule.core.performance.HomeWallpaperImages
import com.xiaomanjun.sleepdownschedule.glass.ui.DefaultCourseCardPalette

/** Only home dock consumers use this accent; toolbar/search spotlights keep their native color. */
internal val LocalColorOsDockAccent = compositionLocalOf<Int?> { null }

internal fun dockAccent(source: ColorOsNativeUi.Source, context: Context, wallpaperAccent: Int?): Int =
    wallpaperAccent ?: source.color(context, "couiColorContainerTheme").defaultColor

@Composable
internal fun rememberColorOsDockAccent(images: HomeWallpaperImages): Int? {
    val host = LocalContext.current
    val source = remember(host) { ColorOsNativeUi.source(host) } ?: return null
    val native = rememberNativeContext(dark = false) ?: return null
    return remember(images.source, images.representativeColors, native) {
        val fallback = dockAccent(source, native.second, null)
        if (images.source == null || images.representativeColors == DefaultCourseCardPalette) return@remember fallback
        val seed = images.representativeColors.firstOrNull { color ->
            val hsv = FloatArray(3)
            Color.colorToHSV(color.toInt(), hsv)
            hsv[1] >= 0.16f && hsv[2] >= 0.22f
        } ?: return@remember fallback
        val hsv = FloatArray(3)
        Color.colorToHSV(seed.toInt(), hsv)
        // Keep the wallpaper hue while leaving enough contrast for white plus and small labels.
        hsv[1] = hsv[1].coerceIn(0.50f, 0.85f)
        hsv[2] = hsv[2].coerceIn(0.60f, 0.80f)
        Color.HSVToColor(hsv)
    }
}
