package com.xstudio.app

import com.xstudio.app.photoenhancer.OutputLimit
import com.xstudio.app.photoenhancer.OutputPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputPlanTest {
    private val budget = OutputPlan.MAX_SAFE_PIXELS

    @Test fun landscape16x9_cannotReach8K_andSaysSo() {
        val p = OutputPlan.plan(1920, 1080, "8K", 4320, budget)
        assertFalse(p.reachedRequested)
        assertEquals(OutputLimit.MEMORY_BUDGET, p.limit)
        assertTrue(p.outW.toLong() * p.outH <= budget + 20_000)
        assertEquals("4K", p.achievedLabel)
        assertTrue(p.statusText.contains("NOT reached"))
    }

    @Test fun squareImage_reaches8K() {
        val p = OutputPlan.plan(2000, 2000, "8K", 4320, budget)
        assertTrue(p.reachedRequested)
        assertEquals(4320, p.outW)
        assertEquals("8K", p.achievedLabel)
    }

    @Test fun smallRequest_isExact() {
        val p = OutputPlan.plan(1920, 1080, "1080p", 1080, budget)
        assertEquals(1920, p.outW); assertEquals(1080, p.outH)
        assertTrue(p.reachedRequested)
    }

    @Test fun originalKeepsSize_whenWithinBudget() {
        val p = OutputPlan.plan(4000, 3000, "Original", null, budget)
        assertEquals(4000, p.outW); assertEquals(3000, p.outH)
        assertEquals(OutputLimit.NONE, p.limit)
    }

    @Test fun neverExceedsEdgeCap() {
        val p = OutputPlan.plan(4000, 500, "8K", 4320, budget)
        assertTrue(maxOf(p.outW, p.outH) <= OutputPlan.MAX_EDGE_PX)
    }
}
