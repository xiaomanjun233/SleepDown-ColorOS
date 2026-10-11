package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier

/** App assembly supplies vendor UI; the shared design system does not depend on a feature. */
internal data class NativeAlertRequest(
    val title: String,
    val message: String,
    val actions: List<LiquidAlertAction>,
    val dark: Boolean,
    val onDismissRequest: () -> Unit,
    val messageMaxHeight: Dp,
    val scrollableMessageContent: Boolean,
    val messageContent: (@Composable () -> Unit)?,
    val visible: Boolean = true,
    val deferDismissUntilHost: Boolean = false
)

internal val LocalNativeAlertRenderer = compositionLocalOf<(@Composable (NativeAlertRequest) -> Boolean)?> { null }
internal val LocalNativeDialogSurface = compositionLocalOf { false }

internal data class NativeDialogButtonRequest(val label: String, val primary: Boolean, val dark: Boolean,
    val enabled: Boolean, val onClick: () -> Unit, val modifier: Modifier, val height: Dp, val iconRes: Int?)
internal val LocalNativeDialogButtonRenderer = compositionLocalOf<(@Composable (NativeDialogButtonRequest) -> Boolean)?> { null }
