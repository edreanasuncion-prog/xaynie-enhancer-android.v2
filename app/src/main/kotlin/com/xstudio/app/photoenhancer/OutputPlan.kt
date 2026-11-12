package com.xstudio.app.photoenhancer

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Why the output ended up smaller than the requested tier. */
enum class OutputLimit { NONE, MAX_EDGE, MEMORY_BUDGET }

/**
 * The real output size for one photo. Pure arithmetic (no Android types) so it can be unit-tested.
 *
 * [decodedW]/[decodedH] are the pixels actually available after memory-safe subsampled decoding.
 */
data class OutputPlan(
    val decodedW: Int,
    val decodedH: Int,
    val outW: Int,
    val outH: Int,
    val requestedLabel: String,
    val requestedShortSidePx: Int?,
    val limit: OutputLimit
) {
    val megapixels: Double get() = outW.toDouble() * outH.toDouble() / 1_000_000.0

    /** True when the requested tier (e.g. 8K) was actually reached. */
    val reachedRequested: Boolean
        get() = requestedShortSidePx == null || min(outW, outH) >= requestedShortSidePx - 1

    /** Honest name of what was produced, based on the real short side. */
    val achievedLabel: String get() = tierLabelForShortSide(min(outW, outH))

    /** "5658×4244 px (24.0 MP)" */
    val dimensionsText: String
        get() = "$outW×$outH px (${"%.1f".format(megapixels)} MP)"

    /** Text for history rows and the status line. Never claims a tier that was not reached. */
    val historyLabel: String
        get() = if (requestedShortSidePx == null) {
            "${outW}x$outH (${achievedLabel})"
        } else if (reachedRequested) {
            "${outW}x$outH ($requestedLabel)"
        } else {
            "${outW}x$outH (requested $requestedLabel, got ${achievedLabel})"
        }

    val statusText: String
        get() {
            val base = "Output $dimensionsText"
            return when {
                requestedShortSidePx == null && limit != OutputLimit.NONE ->
                    "$base · reduced from the source to stay within the memory-safety limit"
                requestedShortSidePx == null -> "$base · same size as the decoded source"
                reachedRequested -> "$base · $requestedLabel reached"
                else -> {
                    val why = when (limit) {
                        OutputLimit.MAX_EDGE -> "the 8192 px per-side limit"
                        OutputLimit.MEMORY_BUDGET -> "the memory-safety pixel limit"
                        OutputLimit.NONE -> "the requested size"
                    }
                    "$base · $requestedLabel was NOT reached (limited by $why), real size is ${achievedLabel}"
                }
            }
        }

    companion object {
        const val MAX_EDGE_PX = 8192
        const val MIN_SAFE_PIXELS = 1920 * 1080
        const val MAX_SAFE_PIXELS = 24_000_000

        private val tiers = listOf(
            "8K" to 4320,
            "4K" to 2160,
            "2K" to 1440,
            "1080p" to 1080,
            "720p" to 720
        )

        fun tierLabelForShortSide(shortSidePx: Int): String =
            tiers.firstOrNull { shortSidePx >= it.second - 1 }?.first ?: "below 720p"

        /** Pixel budget derived from free heap, clamped exactly like the original implementation. */
        fun safePixelBudget(availableHeapBytes: Long): Int =
            (availableHeapBytes / 10).toInt().coerceIn(MIN_SAFE_PIXELS, MAX_SAFE_PIXELS)

        fun plan(
            decodedW: Int,
            decodedH: Int,
            requestedLabel: String,
            requestedShortSidePx: Int?,
            maxSafePixels: Int
        ): OutputPlan {
            val iw = decodedW.coerceAtLeast(1)
            val ih = decodedH.coerceAtLeast(1)
            val shortSide = min(iw, ih)
            val targetShort = requestedShortSidePx ?: shortSide

            val wanted = targetShort.toDouble() / shortSide.toDouble()
            val edgeCap = MAX_EDGE_PX.toDouble() / max(iw, ih)
            val memCap = sqrt(maxSafePixels.toDouble() / (iw.toDouble() * ih.toDouble()))

            val scale = min(wanted, min(edgeCap, memCap))
            val limit = when {
                scale >= wanted -> OutputLimit.NONE
                edgeCap <= memCap -> OutputLimit.MAX_EDGE
                else -> OutputLimit.MEMORY_BUDGET
            }

            return OutputPlan(
                decodedW = iw,
                decodedH = ih,
                outW = (iw * scale).roundToInt().coerceAtLeast(1),
                outH = (ih * scale).roundToInt().coerceAtLeast(1),
                requestedLabel = requestedLabel,
                requestedShortSidePx = requestedShortSidePx,
                limit = limit
            )
        }
    }
}
