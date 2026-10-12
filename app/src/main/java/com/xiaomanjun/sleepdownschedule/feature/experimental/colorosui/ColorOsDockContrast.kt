package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

private fun contrastShadowColor(foreground: Int): Int =
    if (Color.red(foreground) + Color.green(foreground) + Color.blue(foreground) > 382)
        0x70000000 else 0xB0FFFFFF.toInt()

/** Cache a small glyph alpha mask; native state, scale and touch still belong to COUI. */
internal class DockContrastIcon(icon: Drawable, private val density: Float,
    private var colors: ColorStateList) : DrawableWrapper(icon) {
    private var enabled = false
    private var shadow: Bitmap? = null
    private val offset = IntArray(2)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun setContrastEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        shadow = null
        invalidateSelf()
    }

    override fun setTintList(tint: ColorStateList?) {
        super.setTintList(tint)
        if (tint != null) colors = tint
        shadow = null
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        shadow = null
    }

    override fun onStateChange(state: IntArray): Boolean {
        val changed = super.onStateChange(state)
        if (changed) shadow = null
        return changed
    }

    override fun draw(canvas: Canvas) {
        if (enabled && bounds.width() > 0 && bounds.height() > 0) {
            if (shadow == null) {
                val glyph = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
                val maskCanvas = Canvas(glyph)
                maskCanvas.translate(-bounds.left.toFloat(), -bounds.top.toFloat())
                super.draw(maskCanvas)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    maskFilter = BlurMaskFilter(1.5f * density, BlurMaskFilter.Blur.NORMAL)
                }
                shadow = glyph.extractAlpha(maskPaint, offset)
            }
            val foreground = colors.getColorForState(state, colors.defaultColor)
            shadowPaint.color = contrastShadowColor(foreground)
            canvas.drawBitmap(requireNotNull(shadow), bounds.left + offset[0].toFloat(),
                bounds.top + offset[1].toFloat(), shadowPaint)
        }
        super.draw(canvas)
    }
}

internal fun applyDockTextContrast(root: View, enabled: Boolean) {
    val radius = if (enabled) 1.5f * root.resources.displayMetrics.density else 0f
    fun visit(view: View) {
        if (view is TextView) {
            val color = if (enabled) contrastShadowColor(view.currentTextColor) else Color.TRANSPARENT
            if (view.shadowRadius != radius || view.shadowColor != color || view.shadowDx != 0f || view.shadowDy != 0f)
                view.setShadowLayer(radius, 0f, 0f, color)
        }
        if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
    }
    visit(root)
}
