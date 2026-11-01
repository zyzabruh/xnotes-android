package com.xnotes.core.stroke

import com.xnotes.core.geometry.Pt
import kotlin.math.hypot

/**
 * The pen stabiliser: the nib's raw path pushed through the engine's own delay-compensated
 * low-pass ([StrokeEngine.emaStep]) as it arrives, so a shaky hand writes straighter than it
 * draws.
 *
 * This is the same filter the ribbon applies over a finished stroke, run a sample at a time on
 * the way in instead of afterwards, so what the page keeps is already the steadier path: nothing
 * can later resteady ink that is down, and turning the setting off draws exactly as it always
 * has. It is delay-compensated, so a stroke keeps its shape and only trails the nib rather than
 * being pulled about; what it buys is that a jitter too fast for the eye to follow never lands,
 * because a run that short has no arc left to carry it.
 *
 * Lengths are viewport px: the filter smooths per unit of travel, so one setting steadies a
 * stroke at any zoom, at any hand speed, on any device.
 */
class PenStabilizer {

    /** How far the drawn path trails the nib, in viewport px. 0 passes the raw path through. */
    var lambda: Double = 0.0
        set(value) {
            field = if (value.isFinite() && value > 0.0) value else 0.0
        }

    /** Whether a stroke has opened the filter; a step before pen-down seeds it instead. */
    private var started = false

    /** The previous raw sample, which the travel a step integrates over is measured from. */
    private var lastX = 0.0
    private var lastY = 0.0

    /** The filtered path so far. */
    private var outX = 0.0
    private var outY = 0.0

    /** Open a stroke at its pen-down point, so the path starts exactly under the nib. */
    fun begin(x: Double, y: Double) {
        started = true
        lastX = x
        lastY = y
        outX = x
        outY = y
    }

    /** Where the nib's path draws, having arrived at the raw sample [x], [y]. */
    fun step(x: Double, y: Double): Pt {
        if (!started) { // a step before pen-down: open there rather than jump to it
            begin(x, y)
            return Pt(x, y)
        }
        if (lambda <= 0.0) { // off: follow the hand exactly, as every build has
            lastX = x
            lastY = y
            outX = x
            outY = y
            return Pt(x, y)
        }
        val travel = hypot(x - lastX, y - lastY)
        outX = StrokeEngine.emaStep(outX, lastX, x, travel, lambda)
        outY = StrokeEngine.emaStep(outY, lastY, y, travel, lambda)
        lastX = x
        lastY = y
        return Pt(outX, outY)
    }

    companion object {
        /** The lag the slider's top buys, in viewport px: far past the digitiser's shake, still
         *  short enough that the nib stays ahead of the ink it is laying down. */
        const val MAX_LAMBDA_PX = 16.0

        /** The slider's position, 0 (off) to 100 (high), as the lag it buys in viewport px. */
        fun lambdaFor(percent: Double): Double {
            if (!percent.isFinite()) return 0.0
            return MAX_LAMBDA_PX * percent.coerceIn(0.0, 100.0) / 100.0
        }
    }
}