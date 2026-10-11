package com.xiaomanjun.sleepdownschedule.feature.experimental.colorosui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext

/** Leave the actual content behind native buttons; the header blur is for header text only. */
@Composable
internal fun Modifier.colorOsHeaderMaterialCutouts(buttons: List<Rect>): Modifier {
    val host = LocalContext.current
    if (remember(host) { ColorOsNativeUi.source(host) } == null) return this
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.boundsInRoot().topLeft }.drawWithContent {
        val path = Path()
        buttons.filter { it.width > 0f && it.height > 0f }.forEach { bounds ->
            val local = bounds.translate(-origin)
            path.addRoundRect(RoundRect(local, CornerRadius(minOf(local.width, local.height) / 2f)))
        }
        clipPath(path, ClipOp.Difference) { this@drawWithContent.drawContent() }
    }
}
