package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import android.app.Activity
import android.app.Dialog as AndroidDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertAction
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeDialogSurface
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeAlertRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeDialogButtonRequest
import com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme
import com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

/** Native COUI owns the window, surface, scrolling message, button layout and touch feedback. */
@Composable
internal fun ColorOsAlertDialog(request: NativeAlertRequest): Boolean {
    val native = rememberNativeContext(request.dark) ?: return false
    val source = native.first
    val context = native.second
    val host = LocalContext.current
    val owner = LocalView.current
    val parentComposition = rememberCompositionContext()
    val latest = rememberUpdatedState(request)
    val actionLayout = request.actions.map { it.label to it.style }
    var showFailed by remember(native, request.dark, actionLayout) { mutableStateOf(false) }
    val control = remember(native, request.dark, actionLayout, request.messageContent != null, request.deferDismissUntilHost) {
        ColorOsNativeUi.prepare("create a system alert") {
            source.updateTheme(context, request.dark)
            val builderType = source.load("com.coui.appcompat.dialog.COUIAlertDialogBuilder")
            // Use COUI's default dialog, including its own placement and window animation style.
            val builder = builderType.getConstructor(Context::class.java).newInstance(context)
            builderType.getMethod("setTitle", CharSequence::class.java).invoke(builder, request.title)
            if (request.message.isNotBlank()) builderType.getMethod("setMessage", CharSequence::class.java)
                .invoke(builder, request.message)
            builderType.getMethod("setBlurBackgroundDrawable", Boolean::class.javaPrimitiveType).invoke(builder, true)
            check(request.actions.size <= 3) { "System alerts support up to three actions" }
            val recommended = request.actions.indexOfLast { it.style == LiquidAlertActionStyle.Primary }
                .takeIf { it >= 0 } ?: request.actions.lastIndex
            val recommendType = source.load("com.coui.appcompat.button.COUIButton\$ButtonType")
            builderType.getMethod("setRecommendButtonType", recommendType).invoke(builder,
                recommendType.getField(if (request.actions.getOrNull(recommended)?.style == LiquidAlertActionStyle.Primary)
                    "PRIMARY" else "SECONDARY").get(null))
            val otherSlots = listOf(DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL).iterator()
            val slots = request.actions.indices.map { index ->
                if (index == recommended) DialogInterface.BUTTON_POSITIVE else otherSlots.next()
            }
            request.actions.forEachIndexed { index, action ->
                val method = when (slots[index]) {
                    DialogInterface.BUTTON_NEGATIVE -> "setNegativeButton"
                    DialogInterface.BUTTON_NEUTRAL -> "setNeutralButton"
                    else -> "setPositiveButton"
                }
                builderType.getMethod(method, CharSequence::class.java, DialogInterface.OnClickListener::class.java)
                    .invoke(builder, action.label, null)
            }
            val compose = if (request.messageContent != null) ComposeView(host).apply {
                id = View.generateViewId()
                setViewTreeLifecycleOwner(owner.findViewTreeLifecycleOwner())
                setViewTreeSavedStateRegistryOwner(owner.findViewTreeSavedStateRegistryOwner())
                setViewTreeViewModelStoreOwner(owner.findViewTreeViewModelStoreOwner())
                setParentCompositionContext(parentComposition)
                setContent {
                    val current = latest.value
                    CompositionLocalProvider(LocalNativeDialogSurface provides true) {
                        Box(Modifier.fillMaxWidth().heightIn(max = current.messageMaxHeight)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .then(if (current.scrollableMessageContent) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
                            current.messageContent?.invoke()
                        }
                    }
                }
                builderType.getMethod("setView", View::class.java).invoke(builder, this)
            } else null
            val dialog = builderType.getMethod("create").invoke(builder) as AndroidDialog
            host.activityOrNull()?.let(dialog::setOwnerActivity)
            dialog.window?.let { window ->
                // WindowManager must resolve COUI's animation ID in the APK that owns it.
                // The Activity token, app context and process UID remain SleepDown's.
                window.attributes = window.attributes.apply { packageName = source.packageName }
            }
            NativeAlert(dialog, builder, compose, slots, source, context) { latest.value }
        }
    } ?: return false
    if (showFailed) return false
    DisposableEffect(control) {
        if (ColorOsNativeUi.prepare("show a system alert") { control.show(); true } != true) {
            control.dispose()
            showFailed = true
        }
        onDispose { control.dispose() }
    }
    SideEffect { if (!showFailed) control.update() }
    return true
}

private class NativeAlert(
    val dialog: AndroidDialog,
    private val builder: Any,
    private val compose: ComposeView?,
    private val slots: List<Int>,
    private val source: ColorOsNativeUi.Source,
    private val context: Context,
    private val request: () -> NativeAlertRequest
) {
    private var disposed = false
    private var pending: (() -> Unit)? = null
    private var dismissGuard: NativeFormDismissGuard? = null
    private var displayedTitle: String? = null
    private var displayedMessage: String? = null
    private var actionsBound = false

    fun show() {
        dialog.setOnCancelListener { pending = request().onDismissRequest }
        dialog.setOnDismissListener {
            if (!disposed) {
                val completion = pending ?: request().onDismissRequest
                pending = null
                completion()
            }
        }
        if (request().deferDismissUntilHost) {
            // Outside/back requests may open an unsaved-changes confirmation instead of closing.
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
        }
        dialog.show()
        // COUIAlertDialogBuilder.show() performs this after Dialog.show(), before the first
        // window traversal. create()+show() alone never installs its blur attach listener or
        // initializes the COUIButtonBarLayout. Keep our pre-show ownership/dismiss configuration.
        builder.javaClass.getDeclaredMethod("disabledTitleScroll", source.load("androidx.appcompat.app.AlertDialog"))
            .apply { isAccessible = true }.invoke(builder, dialog)
        builder.javaClass.getMethod("updateViewAfterShown").invoke(builder)
        if (request().deferDismissUntilHost) {
            dismissGuard = NativeFormDismissGuard(dialog, source) { request().onDismissRequest() }
        }
        update()
        Log.i("SleepDownCOUI", "System dialog shown: buttons=${slots.size}, customContent=${compose != null}, " +
            "gravity=${dialog.window?.attributes?.gravity}, animation=${dialog.window?.attributes?.windowAnimations}, " +
            "animationPackage=${dialog.window?.attributes?.packageName}")
        dialog.window?.decorView?.post {
            if (!disposed && dialog.isShowing) ColorOsNativeUi.prepare("inspect attached dialog material") {
                val blur = builder.javaClass.getDeclaredField("mBackgroundBlurBuilder")
                    .apply { isAccessible = true }.get(builder)
                val blurry = blur.javaClass.getMethod("isBlurryState").invoke(blur)
                Log.i("SleepDownCOUI", "System dialog material attached: windowBlur=$blurry, " +
                    "hardware=${dialog.window?.decorView?.isHardwareAccelerated}")
            }
        }
    }

    fun update() {
        if (disposed || !dialog.isShowing) return
        val current = request()
        if (!current.visible) { pending = {}; dialog.dismiss(); return }
        if (displayedTitle != current.title) {
            dialog.setTitle(current.title)
            displayedTitle = current.title
        }
        if (compose == null && displayedMessage != current.message) {
            dialog.javaClass.getMethod("setMessage", CharSequence::class.java).invoke(dialog, current.message)
            displayedMessage = current.message
        }
        current.actions.forEachIndexed { index, action ->
            val button = dialog.javaClass.getMethod("getButton", Int::class.javaPrimitiveType)
                .invoke(dialog, slots[index]) as TextView
            if (button.isEnabled != action.enabled) button.isEnabled = action.enabled
            if (!actionsBound && action.style == LiquidAlertActionStyle.Destructive) {
                button.setTextColor(source.color(context, "couiColorError"))
            }
            // Replace only click dispatch; native press/spotlight remains on the original COUIButton.
            if (!actionsBound) button.setOnClickListener {
                val active = request().actions[index]
                if (active.enabled) {
                    if (active.dismissOnClick) { pending = active.onClick; dialog.dismiss() }
                    else active.onClick()
                }
            }
        }
        actionsBound = true
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        pending = null
        dismissGuard?.dispose()
        dismissGuard = null
        dialog.setOnCancelListener(null)
        dialog.setOnDismissListener(null)
        dialog.dismiss()
        compose?.disposeComposition()
    }
}

/** Preserve native inside touch dispatch; only ask the form owner about outside/back dismissal. */
private class NativeFormDismissGuard(dialog: AndroidDialog, source: ColorOsNativeUi.Source,
    private val onDismissRequest: () -> Unit) {
    private val window = checkNotNull(dialog.window)
    private val delegate = checkNotNull(window.callback)
    private val panelId = source.resource("parentPanel", "id")
    private val bounds = Rect()
    private var disposed = false
    private var outsideTouch = false
    private var backCallback: android.window.OnBackInvokedCallback? = null
    private val callback = Proxy.newProxyInstance(delegate.javaClass.classLoader,
        arrayOf(Window.Callback::class.java)) { _, method, args ->
        if (!disposed && method.name == "dispatchTouchEvent") {
            val event = args?.firstOrNull() as? MotionEvent
            val panel = window.decorView.findViewById<View>(panelId)
            if (event != null && panel != null && panel.getGlobalVisibleRect(bounds)) {
                val outside = !bounds.contains(event.rawX.toInt(), event.rawY.toInt())
                if (event.actionMasked == MotionEvent.ACTION_DOWN) outsideTouch = outside
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE || outsideTouch) {
                    if (outside && (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_OUTSIDE))
                        onDismissRequest()
                    if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL)
                        outsideTouch = false
                    return@newProxyInstance true
                }
            }
        }
        try { method.invoke(delegate, *(args ?: emptyArray())) }
        catch (error: InvocationTargetException) { throw error.targetException }
    } as Window.Callback

    init {
        window.callback = callback
        dialog.setOnKeyListener { _, key, event ->
            if (key == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onDismissRequest()
                true
            } else false
        }
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = android.window.OnBackInvokedCallback { if (!disposed) onDismissRequest() }.also {
                window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, it)
            }
        }
    }

    fun dispose() {
        disposed = true
        if (window.callback === callback) window.callback = delegate
        if (Build.VERSION.SDK_INT >= 33) backCallback?.let {
            window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
        }
        backCallback = null
    }
}

/** Full forms retain their business content and picker host inside COUI's original dialog. */
@Composable
internal fun ColorOsOrComposeDialog(config: ScheduleConfigEntity, onDismissRequest: () -> Unit, visible: Boolean = true,
    content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val maxHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() * 0.82f }
    if (ColorOsAlertDialog(NativeAlertRequest("", "", emptyList(), appUsesDarkTheme(config),
            onDismissRequest, maxHeight, false, content, visible, deferDismissUntilHost = true))) return
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false), content = content)
}

@Composable
internal fun ColorOsNameDialog(title: String, initialName: String, dark: Boolean, maxLength: Int?,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit): Boolean {
    val native = rememberNativeContext(dark) ?: return false
    var value by remember(initialName) { mutableStateOf(initialName) }
    val edit = remember(native, initialName, maxLength, dark) {
        ColorOsNativeUi.prepare("create a native schedule name field") {
            native.first.updateTheme(native.second, dark)
            (native.first.load("com.coui.appcompat.edittext.COUIEditText").getConstructor(Context::class.java)
                .newInstance(native.second) as EditText).apply {
                hint = "课表名称"
                contentDescription = "课表名称"
                setSingleLine(true)
                maxLength?.let { filters = arrayOf(android.text.InputFilter.LengthFilter(it)) }
                setText(value)
                setSelection(text.length)
            }
        }
    } ?: return false
    val content: @Composable () -> Unit = {
        val current = rememberUpdatedState(value)
        DisposableEffect(edit) {
            val watcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.toString() != current.value) value = s?.toString().orEmpty()
                }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            }
            edit.addTextChangedListener(watcher)
            onDispose { edit.removeTextChangedListener(watcher) }
        }
        AndroidView(factory = { edit }, modifier = Modifier.fillMaxWidth())
    }
    return ColorOsAlertDialog(NativeAlertRequest(title, "", listOf(
        LiquidAlertAction("取消", LiquidAlertActionStyle.Secondary, onClick = onDismiss),
        LiquidAlertAction("完成", LiquidAlertActionStyle.Primary, onClick = { onConfirm(value.trim()) })
    ), dark, onDismiss, 120.dp, false, content))
}

private tailrec fun Context.activityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activityOrNull()
    else -> null
}

@Composable
internal fun ColorOsDialogButton(request: NativeDialogButtonRequest): Boolean {
    request.iconRes?.let { icon ->
        return ColorOsHomeIconButton(icon, request.label, request.dark, request.enabled, false,
            request.modifier, request.onClick, enabled = request.enabled)
    }
    val native = rememberNativeContext(request.dark) ?: return false
    val click = rememberUpdatedState(request.onClick)
    val button = remember(native, request.dark, request.primary) {
        ColorOsNativeUi.prepare("create a native dialog button") {
            native.first.updateTheme(native.second, request.dark)
            val type = native.first.load("com.coui.appcompat.button.COUIButton")
            val view = type.getConstructor(Context::class.java).newInstance(native.second) as TextView
            val buttonType = native.first.load("${type.name}\$ButtonType")
            val kind = buttonType.getField(if (request.primary) "PRIMARY" else "SECONDARY").get(null)
            type.getMethod("setButtonType", buttonType).invoke(view, kind)
            val materialState = native.first.load("${type.name}\$MaterialEffectState")
            // Pass the type together with the material state: the one-argument setter resets
            // the button type to UNKNOWN. Custom form buttons are outside COUI's button panel.
            type.getMethod("setUseMaterialEffectAttr", materialState, buttonType).invoke(view,
                materialState.getField("FOLLOW_SYSTEM").get(null),
                kind)
            type.getMethod("setIsDrawableMaterialEffect", Boolean::class.javaPrimitiveType).invoke(view, false)
            view
        }
    } ?: return false
    AndroidView(factory = { button }, modifier = request.modifier.height(request.height), update = {
        it.text = request.label
        it.isEnabled = request.enabled
        it.setOnClickListener { click.value() }
    })
    DisposableEffect(button) { onDispose { button.setOnClickListener(null) } }
    return true
}
