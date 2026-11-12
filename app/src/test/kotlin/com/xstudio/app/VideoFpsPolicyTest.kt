package com.xstudio.app

import com.xstudio.app.data.model.VideoFpsOption
import com.xstudio.app.videoenhancer.VideoFpsPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFpsPolicyTest {
    private fun enabled(src: Float?) =
        VideoFpsPolicy.choices(src).filter { it.enabled }.map { it.option }

    @Test fun source30_offersOnlySourceAndSame30() {
        assertEquals(listOf(VideoFpsOption.SOURCE, VideoFpsOption.FPS_30), enabled(30f))
    }

    @Test fun source60_offers30And60_not90or120() {
        val e = enabled(60f)
        assertTrue(VideoFpsOption.FPS_30 in e && VideoFpsOption.FPS_60 in e)
        assertFalse(VideoFpsOption.FPS_90 in e || VideoFpsOption.FPS_120 in e)
    }

    @Test fun unknownSource_onlySource() {
        assertEquals(listOf(VideoFpsOption.SOURCE), enabled(null))
    }

    @Test fun reductionDrops_equalDoesNot_raiseIsNull() {
        assertEquals(30f, VideoFpsPolicy.plan(60f, VideoFpsOption.FPS_30)!!.dropToFps)
        assertNull(VideoFpsPolicy.plan(30f, VideoFpsOption.FPS_30)!!.dropToFps)
        assertNull(VideoFpsPolicy.plan(29.97f, VideoFpsOption.FPS_30)!!.dropToFps)
        assertNull(VideoFpsPolicy.plan(30f, VideoFpsOption.FPS_120))
        assertNotNull(VideoFpsPolicy.plan(30f, VideoFpsOption.SOURCE))
    }

    @Test fun sanitize_fallsBackToSource() {
        assertEquals(VideoFpsOption.SOURCE, VideoFpsPolicy.sanitize(30f, VideoFpsOption.FPS_120))
        assertEquals(VideoFpsOption.FPS_30, VideoFpsPolicy.sanitize(60f, VideoFpsOption.FPS_30))
    }
}
