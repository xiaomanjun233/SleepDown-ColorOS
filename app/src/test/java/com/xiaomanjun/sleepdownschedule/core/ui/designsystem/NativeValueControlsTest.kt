package com.xiaomanjun.sleepdownschedule.core.ui.designsystem

import androidx.compose.ui.Modifier
import org.junit.Assert.*
import org.junit.Test

class NativeValueControlsTest {
    @Test fun dragPreviewsThenCommitsExactlyOnceBeforePreviewCloses() {
        val events = mutableListOf<String>()
        val request = NativeSliderRequest({ 30f }, 0f..100f,
            { events += "preview:$it" }, { events += "active:$it" },
            { events += "commit:$it" }, Modifier)
        val session = NativeSliderSession { request }
        session.start(); session.start(); session.change(70f); session.finish(false); session.finish(false)
        assertEquals(listOf("active:true", "preview:70.0", "commit:70.0", "active:false"), events)
    }
    @Test fun cancelRestoresTheStartValueWithoutCommitting() {
        val events = mutableListOf<String>()
        val request = NativeSliderRequest({ 30f }, 0f..100f,
            { events += "preview:$it" }, { events += "active:$it" },
            { events += "commit:$it" }, Modifier)
        val session = NativeSliderSession { request }
        session.start(); session.change(70f); session.finish(true)
        assertEquals(listOf("active:true", "preview:70.0", "preview:30.0", "active:false"), events)
    }
    @Test fun accessibilityChangesCommitWithoutAStuckTouchPreview() {
        var committed = 0f
        val session = NativeSliderSession {
            NativeSliderRequest({ 30f }, 0f..100f, {}, { fail("No touch preview expected") }, { committed = it }, Modifier)
        }
        session.change(200f)
        assertEquals(100f, committed, 0f)
        assertFalse(session.tracking)
    }
    @Test fun blurMinimumDefaultAndMaximumSurviveDisplayRoundTrips() {
        assertEquals(0.2f, nativeCardBlurScale(0f), 0.0001f)
        assertEquals(1f, nativeCardBlurScale(50f), 0.0001f)
        assertEquals(2f, nativeCardBlurScale(100f), 0.0001f)
        assertEquals(0f, nativeCardBlurPercent(0f), 0.0001f)
        listOf(0.2f, 0.5f, 1f, 1.6f, 2f).forEach {
            assertEquals(it, nativeCardBlurScale(nativeCardBlurPercent(it)), 0.0001f)
        }
    }
}
