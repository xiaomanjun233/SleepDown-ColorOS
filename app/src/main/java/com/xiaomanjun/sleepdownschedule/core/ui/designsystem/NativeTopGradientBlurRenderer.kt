package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** A top-to-bottom system app-bar material; the page retains sizing and snapshot ownership. */
internal val LocalNativeTopGradientBlurRenderer =
    staticCompositionLocalOf<(@Composable (Modifier, Boolean) -> Boolean)?> { null }
