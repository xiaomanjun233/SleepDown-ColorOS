package com.xiaomanjun.sleepdownschedule.coloros

import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.LiquidAlertActionStyle

internal enum class ColorOsAlertSlot { Positive, Negative, Neutral }

/** COUI's vertical order is neutral, positive, negative; never derive cancellation from text. */
internal fun colorOsAlertSlots(styles: List<LiquidAlertActionStyle>): List<ColorOsAlertSlot> {
    require(styles.size <= 3)
    if (styles.isEmpty()) return emptyList()
    val cancel = styles.indexOfLast { it == LiquidAlertActionStyle.Cancel }
    val positive = styles.indexOfLast { it == LiquidAlertActionStyle.Primary }.takeIf { it >= 0 }
        ?: styles.indices.lastOrNull { it != cancel } ?: styles.lastIndex
    val negative = cancel.takeIf { it >= 0 && it != positive }
        ?: styles.indices.firstOrNull { it != positive }
    return styles.indices.map {
        when (it) {
            positive -> ColorOsAlertSlot.Positive
            negative -> ColorOsAlertSlot.Negative
            else -> ColorOsAlertSlot.Neutral
        }
    }
}
