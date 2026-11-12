package com.xstudio.app.videoenhancer

import com.xstudio.app.data.model.VideoFpsOption
import kotlin.math.abs
import kotlin.math.roundToInt

/** One selectable frame-rate chip and whether it can really be produced for the current clip. */
data class FpsChoice(val option: VideoFpsOption, val enabled: Boolean, val note: String?)

/**
 * What the export will actually do about frame rate.
 * [dropToFps] is non-null only when frames will really be dropped (FrameDropEffect).
 */
data class FpsPlan(val dropToFps: Float?, val description: String)

/**
 * Frame-rate rules. Media3's FrameDropEffect can only remove frames, so:
 *  - a target BELOW the source rate is a real reduction (frames dropped);
 *  - a target within [EQUAL_TOLERANCE] of the source is a no-op (same rate, nothing dropped);
 *  - a target ABOVE the source is impossible here (no interpolation) and is never offered;
 *  - if the source rate could not be detected, only "Source FPS" is offered.
 * No Android types, so this is unit-testable.
 */
object VideoFpsPolicy {
    const val EQUAL_TOLERANCE = 1.0f

    fun choices(sourceFps: Float?): List<FpsChoice> = VideoFpsOption.entries.map { option ->
        when {
            option == VideoFpsOption.SOURCE -> FpsChoice(option, true, null)
            sourceFps == null -> FpsChoice(option, false, "Source frame rate unknown")
            option.fps > sourceFps + EQUAL_TOLERANCE ->
                FpsChoice(option, false, "Source is ~${fmt(sourceFps)} fps; frames cannot be created")
            abs(option.fps - sourceFps) <= EQUAL_TOLERANCE -> FpsChoice(option, true, "Same as source")
            else -> FpsChoice(option, true, null)
        }
    }

    /** Null when [requested] cannot be produced for this source. */
    fun plan(sourceFps: Float?, requested: VideoFpsOption): FpsPlan? {
        if (requested == VideoFpsOption.SOURCE) {
            val rate = sourceFps?.let { "~${fmt(it)} fps" } ?: "its original rate"
            return FpsPlan(null, "Frame rate kept at the source's $rate.")
        }
        val src = sourceFps ?: return null
        return when {
            requested.fps > src + EQUAL_TOLERANCE -> null
            abs(requested.fps - src) <= EQUAL_TOLERANCE ->
                FpsPlan(null, "Source is already ~${fmt(src)} fps; no frames dropped.")
            else -> FpsPlan(
                requested.fps.toFloat(),
                "Frames dropped from ~${fmt(src)} fps down to at most ${requested.fps} fps. No frames are created or interpolated."
            )
        }
    }

    /** Falls back to SOURCE if the chosen option stopped being valid (for example a new clip was picked). */
    fun sanitize(sourceFps: Float?, requested: VideoFpsOption): VideoFpsOption =
        if (plan(sourceFps, requested) != null) requested else VideoFpsOption.SOURCE

    private fun fmt(v: Float): String =
        if (abs(v - v.roundToInt()) < 0.05f) v.roundToInt().toString() else "%.2f".format(v)
}
