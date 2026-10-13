package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ColorOsSlider(request: NativeSliderRequest): Boolean {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val native = rememberNativeContext(dark) ?: return false
    val latest = rememberUpdatedState(request)
    val session = remember(native, request.range) { NativeSliderSession { latest.value } }
    val binding = remember(native, session) {
        ColorOsNativeUi.prepare("create native value slider") {
            NativeSliderBinding(native.first, native.second, { latest.value }, session)
        }
    } ?: return false
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(factory = { binding.view }, modifier = request.modifier.fillMaxWidth().height(48.dp), update = {
        it.isEnabled = request.enabled
        binding.sync(request.value(), accent)
    })
    DisposableEffect(binding) { onDispose { binding.dispose() } }
    return true
}

private class NativeSliderBinding(source: ColorOsNativeUi.Source, context: Context,
    private val request: () -> NativeSliderRequest, private val session: NativeSliderSession) {
    private val type = source.load("com.coui.appcompat.seekbar.COUISeekBar")
    val view = type.getConstructor(Context::class.java).newInstance(context) as View
    private val listenerType = source.load("${type.name}\$OnSeekBarChangeListener")
    private val setListener = type.getMethod("setOnSeekBarChangeListener", listenerType)
    private val setProgress = type.getMethod("setProgress", Int::class.javaPrimitiveType)
    private val getProgress = type.getMethod("getProgress")
    private var cancelled = false
    private var color: Int? = null
    init {
        type.getMethod("setMax", Int::class.javaPrimitiveType).invoke(view, 10000)
        setListener.invoke(view, source.listener(listenerType.name) { name, args ->
            when (name) {
                "onStartTrackingTouch" -> session.start()
                "onProgressChanged" -> if (args?.getOrNull(2) == true) {
                    val range = request().range
                    var value = range.start + ((args[1] as Int) / 10000f) * (range.endInclusive - range.start)
                    val snap = request().snapValue
                    if (snap != null && abs(value - snap) < (range.endInclusive - range.start) * 0.015f) value = snap
                    session.change(value)
                }
                "onStopTrackingTouch" -> session.finish(cancelled)
            }
            null
        })
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) cancelled = false
            if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                cancelled = true
                view.post { if (cancelled) session.finish(true) }
            }
            false
        }
        source.bindFollowLightIntensity(view)
    }
    fun sync(value: Float, accent: Int) {
        if (!session.tracking) {
            val range = request().range
            val span = range.endInclusive - range.start
            val progress = if (span > 0f) ((value - range.start) / span * 10000).roundToInt().coerceIn(0, 10000) else 0
            if (getProgress.invoke(view) != progress) setProgress.invoke(view, progress)
        }
        if (color != accent) {
            type.getMethod("setProgressColor", ColorStateList::class.java).invoke(view, ColorStateList.valueOf(accent))
            color = accent
        }
    }
    fun dispose() { setListener.invoke(view, null); view.setOnTouchListener(null); session.finish(true) }
}

@Composable
internal fun ColorOsSwitch(request: NativeSwitchRequest): Boolean {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val native = rememberNativeContext(dark) ?: return false
    val latest = rememberUpdatedState(request)
    val view = remember(native) {
        ColorOsNativeUi.prepare("create native switch") {
            (native.first.load("com.coui.appcompat.couiswitch.COUISwitch")
                .getConstructor(Context::class.java).newInstance(native.second) as CompoundButton).also {
                native.first.bindFollowLightIntensity(it)
            }
        }
    } ?: return false
    AndroidView(factory = { view }, modifier = request.modifier, update = {
        it.setOnCheckedChangeListener(null)
        it.isEnabled = request.enabled
        if (it.isChecked != request.checked) it.isChecked = request.checked
        it.setOnCheckedChangeListener { _, checked ->
            if (latest.value.enabled && checked != latest.value.checked) latest.value.onCheckedChange(checked)
        }
    })
    DisposableEffect(view) { onDispose { view.setOnCheckedChangeListener(null) } }
    return true
}

@Composable
internal fun ColorOsSegments(request: NativeSegmentRequest): Boolean {
    if (request.labels.isEmpty()) return false
    val native = rememberNativeContext(request.dark) ?: return false
    val latest = rememberUpdatedState(request)
    val binding = remember(native, request.labels) {
        ColorOsNativeUi.prepare("create native segmented control") {
            val source = native.first
            val type = source.load("com.coui.appcompat.segmentbutton.COUISegmentButtonLayout")
            val view = type.getConstructor(Context::class.java).newInstance(native.second) as View
            type.getMethod("setSegmentButtons", Array<String>::class.java)
                .invoke(view, request.labels.toTypedArray() as Any)
            type.getMethod("setMaterialEffectEnabled", Boolean::class.javaPrimitiveType).invoke(view, true)
            type.getMethod("setSegmentHeight", Float::class.javaPrimitiveType).invoke(view, 42f)
            type.getMethod("selectSegmentAt", Int::class.javaPrimitiveType).invoke(view, request.selected.coerceIn(request.labels.indices))
            val listener = source.load("${type.name}\$OnSegmentSelectedListener")
            type.getMethod("setOnSegmentSelectedListener", listener).invoke(view, source.listener(listener.name) { name, args ->
                if (name == "onSegmentSelected") (args?.firstOrNull() as? Int)?.let {
                    if (it in latest.value.labels.indices && it != latest.value.selected) latest.value.onSelected(it)
                }
                null
            })
            source.bindFollowLightIntensity(view)
            Triple(type, view, listener)
        }
    } ?: return false
    AndroidView(factory = { binding.second }, modifier = request.modifier.height(42.dp), update = {
        val selected = request.selected.coerceIn(request.labels.indices)
        if (binding.first.getMethod("getSelectedSegmentPosition").invoke(it) != selected)
            binding.first.getMethod("selectSegmentAt", Int::class.javaPrimitiveType).invoke(it, selected)
    })
    DisposableEffect(binding) { onDispose {
        binding.first.getMethod("setOnSegmentSelectedListener", binding.third).invoke(binding.second, null)
    } }
    return true
}
