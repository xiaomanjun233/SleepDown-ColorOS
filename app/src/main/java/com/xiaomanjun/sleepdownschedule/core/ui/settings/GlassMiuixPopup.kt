package com.xiaomanjun.sleepdownschedule.core.ui.settings

import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.glass.GlassBackdropDomain
import com.xiaomanjun.sleepdownschedule.glass.GlassEffectFrame
import com.xiaomanjun.sleepdownschedule.glass.GlassMaterialRole
import com.xiaomanjun.sleepdownschedule.glass.GlassMaterialSpec
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassSurfaceDescriptor
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassCombinedBackdrop
import com.xiaomanjun.sleepdownschedule.glass.rememberGlassLayerBackdrop
import com.xiaomanjun.sleepdownschedule.glass.glassBackdropProducer
import com.xiaomanjun.sleepdownschedule.glass.sleepDownGlassSurface
import com.xiaomanjun.sleepdownschedule.glass.ui.appUsesDarkTheme
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.sleepDownPanelForegroundColor
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalCenteredDialogRenderInRootScaffold
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import com.kyant.shapes.RoundedRectangle
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.Constraints
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMenuSurfaceRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativeMenuRowRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuItem
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuRowRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuSurfaceRequest
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMenuTouchSource
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LocalNativePopupRenderer
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativePopupRequest
import androidx.compose.ui.input.pointer.motionEventSpy
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.NativeMaterialSurfaceRequest
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.withTransformCompensation
import com.kyant.backdrop.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.catalog.components.liquidButtonVisualTransform
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.DropdownRowContentRequest
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.ListPopupVisualStyle
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Business model only; Miuix owns popup layout, input and cascading motion. */
@Immutable
internal data class SleepDownLiquidMenuItem(
    val key: String,
    val text: String,
    val iconRes: Int? = null,
    val summary: String? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val accent: Boolean = false,
    val children: List<SleepDownLiquidMenuItem> = emptyList(),
    val onClick: () -> Unit = {}
)

private class UpwardDropdownPositionProvider(
    horizontalSafeInset: Dp,
    private val overlapAnchor: Boolean = false,
    private val morphHorizontalInset: Int = 0,
    private val morphVerticalInset: Int = 0
) : PopupPositionProvider {
    private val margins = PaddingValues(horizontal = horizontalSafeInset, vertical = 8.dp)

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align
    ): IntOffset {
        val endAligned = when (alignment) {
            PopupPositionProvider.Align.End,
            PopupPositionProvider.Align.TopEnd,
            PopupPositionProvider.Align.BottomEnd -> layoutDirection == LayoutDirection.Ltr

            else -> layoutDirection == LayoutDirection.Rtl
        }
        val preferredX = if (endAligned) {
            anchorBounds.right - popupContentSize.width - popupMargin.right + morphHorizontalInset
        } else {
            anchorBounds.left + popupMargin.left - morphHorizontalInset
        }
        val minX = windowBounds.left
        val maxX = (windowBounds.right - popupContentSize.width - popupMargin.right)
            .coerceAtLeast(minX)
        val minY = windowBounds.top + popupMargin.top
        val maxY = (windowBounds.bottom - popupContentSize.height - popupMargin.bottom)
            .coerceAtLeast(minY)
        return IntOffset(
            x = preferredX.coerceIn(minX, maxX),
            y = (if (overlapAnchor) anchorBounds.bottom - popupContentSize.height + morphVerticalInset
                else anchorBounds.top - popupContentSize.height - popupMargin.top)
                .coerceIn(minY, maxY)
        )
    }

    override fun getMargins(): PaddingValues = margins
}

@Composable
private fun Modifier.miuixCascadingPopupSurface(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    blurRadius: Dp
): Modifier {
    val dark = appUsesDarkTheme(config)
    if (com.xiaomanjun.sleepdownschedule.core.performance.AppMaterialPreferences.isSuperPerformance) {
        // The popup host owns its rounded outline and border on every Android version.
        return background(com.xiaomanjun.sleepdownschedule.glass.flatControlColor(dark).copy(alpha = 0.90f))
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || backdrop == null) {
        return background(if (dark) Color(0xFF242424) else Color.White)
    }
    val effectiveBlur = blurRadius.coerceAtMost(24.dp)
    // Keep the lens gentle: an overlarge lens/refraction band refracts content against the
    // rounded popup corners and reads as torn glass lines at the bottom corners.
    val lensHeight = 12.dp
    val lensAmount = 24.dp
    // Match the Home menu's translucent material so the moving light is not buried in white.
    val surfaceAlpha = if (dark) 0.40f else 0.28f
    val surfaceColor = if (dark) Color(0xFF050505) else Color(0xFFF2F4F8)
    val material = GlassMaterialSpec.popup(effectiveBlur).copy(
        lensHeight = lensHeight,
        lensAmount = lensAmount,
        surfaceAlpha = surfaceAlpha,
        borderAlpha = 0f,
        highlightAlpha = 0f,
        shadowAlpha = 0f,
        innerShadowAlpha = 0f,
        depthEffect = false,
        useVibrancy = true
    )
    val descriptor = rememberGlassSurfaceDescriptor(
        debugLabel = "MiuixCascadingPopup",
        domain = GlassBackdropDomain.DialogBridge,
        materialRole = GlassMaterialRole.Popup
    )
    val sampleBackdrop = remember(backdrop) {
        if (backdrop is LayerBackdrop) backdrop.withTransformCompensation() else backdrop
    }
    return sleepDownGlassSurface(
        backdrop = sampleBackdrop,
        descriptor = descriptor,
        material = material,
        // Backdrop's lens shader requires a CornerBasedShape. A zero-radius rounded rect is
        // pixel-identical to RectangleShape while satisfying that runtime contract; Miuix still
        // owns the animated primary/secondary clip paths outside this material layer.
        shape = { RoundedRectangle(0.dp) },
        effectFrame = GlassEffectFrame(
            blur = effectiveBlur,
            lensHeight = lensHeight,
            lensAmount = lensAmount,
            useVibrancy = true,
            chromaticAberration = false,
            highlight = null,
            shadowAlpha = null,
            innerShadow = null,
            depthEffect = false
        ),
        // Keep the Nexio/Miuix effect order and let the surface tint stay light enough for the
        // stronger lens to remain visible through both primary and cascading popup layers.
        effectInputKey = material,
        effectsOverride = remember(effectiveBlur, lensHeight, lensAmount) { {
            vibrancy()
            blur(effectiveBlur.toPx())
            lens(
                lensHeight.toPx(),
                lensAmount.toPx(),
                depthEffect = false,
                chromaticAberration = false
            )
        } },
        onDrawSurface = {
            drawRect(surfaceColor.copy(alpha = surfaceAlpha))
        }
    )
}

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun rememberMiuixListPopupStyle(
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    cornerRadius: Dp = 25.dp
): ListPopupVisualStyle {
    val scope = rememberCoroutineScope()
    val dark = appUsesDarkTheme(config)
    val nativeMaterial = LocalNativeMenuSurfaceRenderer.current != null
    val menuTouch = remember { NativeMenuTouchSource() }
    val radiusCapPx = with(LocalDensity.current) { 90.dp.toPx() }
    val highlight = remember(scope, radiusCapPx, dark) {
        InteractiveHighlight(scope, radius = { minOf(it.minDimension * 0.65f, radiusCapPx) },
            ambientAlpha = if (dark) 0.08f else 0.025f,
            spotAlpha = 0.20f, fallbackAlpha = 0.25f,
            contrastHalo = if (dark) Color.Transparent else Color.Black.copy(alpha = 0.09f),
            fadeOutAtReleasePosition = true)
    }
    // Kyant's observer does not consume input: Miuix continues to own selection and dismissal.
    val visualInteraction = Modifier.liquidButtonVisualTransform(highlight)
    val interaction = visualInteraction.then(highlight.gestureModifier)
    val rim = remember(dark) {
        BorderStroke(1.dp, Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.38f else 0.66f),
            0.07f to Color.White.copy(alpha = 0.10f),
            0.18f to Color.Transparent,
            0.82f to Color.Transparent,
            0.93f to Color.White.copy(alpha = 0.10f),
            1f to Color.White.copy(alpha = if (dark) 0.38f else 0.66f)
        ))
    }
    return ListPopupVisualStyle(
    // Independent size/origin tracks follow Nexio's current popup motion. Sampling remains
    // in the stationary canvas, with our own material and neutral interaction feedback.
    surfaceModifier = if (nativeMaterial) Modifier else Modifier.miuixCascadingPopupSurface(
        backdrop = backdrop,
        config = config,
        blurRadius = 10.dp
    ).then(highlight.modifier),
    surfaceContent = if (nativeMaterial) { modifier, panel ->
        PopupNativeMaterial(modifier, panel, cornerRadius, backdrop, config, menuTouch)
    } else null,
    itemContent = if (nativeMaterial) { row ->
        // Resource-backed business menus supply their own row below; preference choices are text-only.
        if (row.item.icon != null) false else NativePopupRow(
            row, NativeMenuItem(title = row.item.text, description = row.item.summary,
                checkable = row.item.children.isNullOrEmpty(), foreground = sleepDownPanelForegroundColor(config).toArgb()), dark)
    } else null,
    backgroundColor = Color.Transparent,
    cornerRadius = cornerRadius,
    itemTextStyle = MiuixTheme.textStyles.main.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    itemVerticalPadding = 7.dp,
    itemMinHeight = 40.dp,
    border = if (nativeMaterial) null
        else if (com.xiaomanjun.sleepdownschedule.core.performance.AppMaterialPreferences.isSuperPerformance)
        BorderStroke(1.dp, com.xiaomanjun.sleepdownschedule.glass.flatControlBorder(dark))
        else rim,
    morphAnimation = true,
    slideSelection = true,
    dimBackground = false,
    holdAnchor = true,
    // Broad ambient shadow separates the light panel from white settings cards without
    // adding a second blurred backdrop or a dark, hard outline.
    shadowElevation = if (dark) 16.dp else 24.dp,
    ambientShadowColor = Color.Black.copy(alpha = if (dark) 0.125f else 0.22f),
    spotShadowColor = Color.Black.copy(alpha = 0.125f),
    interactionModifier = if (nativeMaterial) Modifier.motionEventSpy(menuTouch::dispatch) else interaction,
    suspendedInteractionModifier = if (nativeMaterial) Modifier else visualInteraction
    )
}

/** Native View occupies the moving panel, rather than the union/shadow allocation canvas. */
@Composable
private fun PopupNativeMaterial(modifier: Modifier, panel: (() -> RoundRect)?, cornerRadius: Dp,
    backdrop: Backdrop?, config: ScheduleConfigEntity, menuTouch: NativeMenuTouchSource) {
    val shape = RoundedRectangle(cornerRadius)
    val placed = if (panel == null) modifier else modifier.layout { measurable, constraints ->
        val rect = panel()
        val child = measurable.measure(Constraints.fixed(rect.width.roundToInt().coerceAtLeast(1),
            rect.height.roundToInt().coerceAtLeast(1)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            child.place(rect.left.roundToInt(), rect.top.roundToInt())
        }
    }
    val rendered = LocalNativeMenuSurfaceRenderer.current?.invoke(NativeMenuSurfaceRequest(NativeMaterialSurfaceRequest(
        modifier = placed, shape = shape, dark = appUsesDarkTheme(config),
        shapeProvider = panel?.let { bounds -> {
            val rect = bounds()
            RoundedCornerShape(CornerSize(rect.topLeftCornerRadius.x), CornerSize(rect.topRightCornerRadius.x),
                CornerSize(rect.bottomRightCornerRadius.x), CornerSize(rect.bottomLeftCornerRadius.x))
        } }
    ), touchSource = menuTouch)) {} == true
    if (!rendered) Box(placed.miuixCascadingPopupSurface(backdrop, config, 10.dp))
}

@Composable
private fun rememberSleepDownPopupRowColors(contentColor: Color? = null): DropdownColors {
    val defaults = DropdownDefaults.dropdownColors()
    val indicatorColor = MiuixTheme.colorScheme.onSurfaceVariantActions
    return remember(defaults, contentColor, indicatorColor) {
        defaults.copy(
            contentColor = contentColor ?: defaults.contentColor,
            summaryColor = contentColor?.copy(alpha = 0.62f) ?: defaults.summaryColor,
            containerColor = Color.Transparent,
            selectedContentColor = contentColor ?: defaults.selectedContentColor,
            selectedSummaryColor = contentColor?.copy(alpha = 0.72f)
                ?: defaults.selectedSummaryColor,
            selectedContainerColor = Color.Transparent,
            selectedIndicatorColor = indicatorColor
        )
    }
}

@Composable
internal fun SleepDownLiquidDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    modifier: Modifier = Modifier,
    summary: String? = null,
    selectedBadgeText: String? = null,
    insideMargin: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    maxHeight: Dp = 318.dp,
    compactTextStyle: TextStyle? = null,
    @Suppress("UNUSED_PARAMETER") expanded: Boolean? = null,
    enabled: Boolean = true,
    showAnchorPressFeedback: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    onSelectedIndexChange: (Int) -> Unit
) {
    var nativeExpanded by remember { mutableStateOf(false) }
    var nativeAnchor by remember { mutableStateOf(Rect.Zero) }
    val nativePopup = LocalNativePopupRenderer.current
    if (nativePopup?.invoke(NativePopupRequest(
            show = nativeExpanded,
            anchorBounds = nativeAnchor,
            items = items.mapIndexed { index, label -> NativeMenuItem(label,
                selected = selectedIndex == index, checkable = true,
                onClick = { onSelectedIndexChange(index) }) },
            dark = appUsesDarkTheme(config),
            onDismissRequest = { nativeExpanded = false; onExpandedChange(false) }
        )) == true) {
        val actualEnabled = enabled && items.isNotEmpty()
        val actionColor = if (actualEnabled) MiuixTheme.colorScheme.onSurfaceVariantActions
            else MiuixTheme.colorScheme.disabledOnSecondaryVariant
        top.yukonga.miuix.kmp.basic.BasicComponent(
            modifier = modifier.onGloballyPositioned { nativeAnchor = it.boundsInRoot() },
            title = if (selectedBadgeText == null && compactTextStyle == null) title else "",
            summary = summary, insideMargin = insideMargin,
            startAction = if (selectedBadgeText != null || compactTextStyle != null) { {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val colors = BasicComponentDefaults.titleColor()
                    MiuixText(title, style = compactTextStyle ?: MiuixTheme.textStyles.main,
                        fontSize = compactTextStyle?.fontSize ?: MiuixTheme.textStyles.headline1.fontSize,
                        fontWeight = if (compactTextStyle == null) FontWeight.Medium else compactTextStyle.fontWeight,
                        color = if (actualEnabled) colors.color else colors.disabledColor)
                    selectedBadgeText?.let { PreferenceBadge(it) }
                }
            } } else null,
            endActions = {
                MiuixText(items.getOrNull(selectedIndex).orEmpty(),
                    modifier = Modifier.padding(end = 8.dp).weight(1f, fill = false),
                    style = compactTextStyle ?: MiuixTheme.textStyles.body2, color = actionColor)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                    tint = actionColor, modifier = Modifier.size(20.dp))
            },
            onClick = { nativeExpanded = true; onExpandedChange(true) },
            enabled = actualEnabled,
            role = androidx.compose.ui.semantics.Role.DropdownList,
            showIndication = showAnchorPressFeedback
        )
        return
    }
    // The dropdown host is rendered by the root Miuix host as a sibling after the page's
    // underlay producer, so it may sample the complete Scaffold underlay (TopBar, large title,
    // content and low-level overlays) instead of only the flat background passed by the caller.
    val completeUnderlayBackdrop = LocalSettingsPopupBackdrop.current ?: backdrop
    val renderInRootScaffold = LocalCenteredDialogRenderInRootScaffold.current
    val popupVisualStyle = rememberMiuixListPopupStyle(completeUnderlayBackdrop, config)
        .copy(holdAnchor = showAnchorPressFeedback).let { style ->
        if (compactTextStyle == null) style else style.copy(
            itemTextStyle = compactTextStyle,
            itemVerticalPadding = 6.dp,
            itemMinHeight = 44.dp
        )
    }
    val popupRowColors = rememberSleepDownPopupRowColors(sleepDownPanelForegroundColor(config))
    val preferenceContent: @Composable () -> Unit = { OverlayDropdownPreference(
        items = items,
        selectedIndex = selectedIndex,
        title = if (selectedBadgeText == null && compactTextStyle == null) title else "",
        modifier = modifier,
        summary = summary,
        startAction = if (selectedBadgeText != null || compactTextStyle != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = BasicComponentDefaults.titleColor()
                    MiuixText(
                        text = title,
                        style = compactTextStyle ?: MiuixTheme.textStyles.main,
                        fontSize = compactTextStyle?.fontSize ?: MiuixTheme.textStyles.headline1.fontSize,
                        fontWeight = if (compactTextStyle == null) FontWeight.Medium
                            else compactTextStyle.fontWeight,
                        color = if (enabled) colors.color else colors.disabledColor
                    )
                    selectedBadgeText?.let { PreferenceBadge(it) }
                }
            }
        } else null,
        insideMargin = insideMargin,
        maxHeight = maxHeight,
        enabled = enabled,
        renderInRootScaffold = renderInRootScaffold,
        excludeFromBackdropCapture = true,
        popupVisualStyle = popupVisualStyle,
        dropdownColors = popupRowColors,
        onExpandedChange = onExpandedChange,
        onSelectedIndexChange = onSelectedIndexChange
    ) }
    if (compactTextStyle == null) {
        preferenceContent()
    } else {
        MiuixTheme(textStyles = MiuixTheme.textStyles.copy(
            main = compactTextStyle,
            body2 = compactTextStyle
        )) { preferenceContent() }
    }
}

@Composable
private fun NativePopupRow(row: DropdownRowContentRequest, item: NativeMenuItem, dark: Boolean): Boolean =
    LocalNativeMenuRowRenderer.current?.invoke(NativeMenuRowRequest(
        item = item.copy(enabled = row.enabled, selected = row.selected), dark = dark,
        modifier = row.modifier, pressed = row.pressed, first = row.first, last = row.last,
        indicatorRotation = row.indicatorRotation
    )) == true

private fun SleepDownLiquidMenuItem.asNativeMenuItem(iconColor: Color): NativeMenuItem = NativeMenuItem(
    title = text, iconRes = iconRes, description = summary, enabled = enabled,
    selected = selected || accent, checkable = selected || accent,
    foreground = iconColor.toArgb(), iconTint = iconColor.toArgb(),
    children = children.map { it.asNativeMenuItem(iconColor) },
    onClick = onClick
)

private fun SleepDownLiquidMenuItem.asMiuixDropdownItem(iconColor: Color, dark: Boolean, nativeRows: Boolean): DropdownItem = DropdownItem(
    text = text,
    enabled = enabled,
    selected = selected || accent,
    onClick = onClick,
    icon = iconRes?.let { resourceId ->
        { modifier ->
            Icon(
                painter = painterResource(resourceId),
                contentDescription = null,
                tint = iconColor,
                modifier = modifier.size(20.dp)
            )
        }
    },
    summary = summary,
    children = children.takeIf { it.isNotEmpty() }?.map { it.asMiuixDropdownItem(iconColor, dark, nativeRows) },
    rowContent = if (nativeRows) { row -> NativePopupRow(row, asNativeMenuItem(iconColor), dark) } else null
)

@Composable
internal fun SleepDownLiquidCascadingPopup(
    show: Boolean,
    anchorBounds: Rect,
    items: List<SleepDownLiquidMenuItem>,
    onDismissRequest: () -> Unit,
    backdrop: Backdrop?,
    config: ScheduleConfigEntity,
    panelMinWidth: Dp = 168.dp,
    menuMaxHeight: Dp? = null,
    horizontalSafeInset: Dp = 0.dp,
    contentColor: Color? = null,
    collapseOnSelection: Boolean = true,
    morphFromAnchor: Boolean = false,
    collapseContent: (@Composable () -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null
) {
    val nativePopup = LocalNativePopupRenderer.current
    if (nativePopup?.invoke(NativePopupRequest(
            show = show, anchorBounds = anchorBounds,
            items = items.map { it.asNativeMenuItem(contentColor ?: sleepDownPanelForegroundColor(config)) },
            dark = appUsesDarkTheme(config), onDismissRequest = onDismissRequest,
            onDismissFinished = onDismissFinished, collapseOnSelection = collapseOnSelection
        )) == true) return
    val completeUnderlayBackdrop = LocalSettingsPopupBackdrop.current ?: backdrop
    val renderInRootScaffold = LocalCenteredDialogRenderInRootScaffold.current
    val primaryPopupBackdrop = rememberGlassLayerBackdrop(
        domain = GlassBackdropDomain.Content,
        providerId = "miuix-cascade-primary"
    )
    val secondaryUnderlayBackdrop: Backdrop = if (completeUnderlayBackdrop != null) {
        rememberGlassCombinedBackdrop(completeUnderlayBackdrop, primaryPopupBackdrop)
    } else {
        primaryPopupBackdrop
    }
    val popupContentColor = contentColor ?: sleepDownPanelForegroundColor(config)
    val dark = appUsesDarkTheme(config)
    val nativeRows = LocalNativeMenuRowRenderer.current != null
    val entry = remember(items, popupContentColor, dark, nativeRows) {
        DropdownEntry(items.map { it.asMiuixDropdownItem(popupContentColor, dark, nativeRows) })
    }
    val basePrimaryVisualStyle = rememberMiuixListPopupStyle(
        backdrop = completeUnderlayBackdrop,
        config = config,
        cornerRadius = 25.dp
    )
    val popupVisualStyle = basePrimaryVisualStyle.copy(
        // Capture the complete parent (including labels) for the child glass. The settings
        // morph renderer draws its material and labels as siblings inside this container.
        interactionModifier = Modifier
            .glassBackdropProducer(primaryPopupBackdrop)
            .then(basePrimaryVisualStyle.interactionModifier),
        suspendedInteractionModifier = Modifier
            .glassBackdropProducer(primaryPopupBackdrop)
            .then(basePrimaryVisualStyle.suspendedInteractionModifier)
    )
    val secondaryPopupVisualStyle = rememberMiuixListPopupStyle(
        backdrop = secondaryUnderlayBackdrop,
        config = config,
        cornerRadius = 25.dp
    )
    val popupRowColors = rememberSleepDownPopupRowColors(
        contentColor ?: sleepDownPanelForegroundColor(config)
    )
    val density = LocalDensity.current
    val popupPositionProvider = remember(horizontalSafeInset, morphFromAnchor, density) {
        UpwardDropdownPositionProvider(horizontalSafeInset, overlapAnchor = morphFromAnchor,
            morphHorizontalInset = if (morphFromAnchor) with(density) { 7.dp.roundToPx() } else 0,
            morphVerticalInset = if (morphFromAnchor) with(density) { 5.dp.roundToPx() } else 0)
    }
    OverlayCascadingListPopup(
        show = show,
        entries = listOf(entry),
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        popupPositionProvider = popupPositionProvider,
        alignment = PopupPositionProvider.Align.End,
        enableWindowDim = false,
        minWidth = panelMinWidth,
        maxHeight = menuMaxHeight,
        renderInRootScaffold = renderInRootScaffold,
        excludeFromBackdropCapture = true,
        visualStyle = popupVisualStyle,
        secondaryVisualStyle = secondaryPopupVisualStyle,
        dropdownColors = popupRowColors,
        // Miuix renders the animated entry in the root scaffold, but the composer/field can still
        // be a later sibling during the first reveal frame. Keep the popup entry above that input
        // layer for the entire enter/exit handoff.
        popupModifier = Modifier.zIndex(1000f),
        collapseOnSelection = collapseOnSelection,
        morphFromAnchor = morphFromAnchor,
        collapseContent = collapseContent
    )
}
