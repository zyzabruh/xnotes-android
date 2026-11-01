package com.xnotes.core.stroke

import com.xnotes.core.geometry.Pt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * [PenStabilizer] turns the nib's jittery raw path into a steadier one for issue #392.
 * Everything is checked in viewport px, which is the space the filter runs in.
 */
class PenStabilizerTest {

    @Test
    fun `zero stabilisation passes the path through untouched`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(0.0)
        s.begin(10.0, 20.0)
        val raw = listOf(Pt(11.0, 21.5), Pt(13.0, 19.0), Pt(12.0, 24.0), Pt(40.0, 5.0))
        for (p in raw) assertEquals(p, s.step(p.x, p.y))
    }

    @Test
    fun `the pen-down sample opens the filter exactly under the nib`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(100.0)
        s.begin(10.0, 20.0)
        assertEquals(Pt(10.0, 20.0), s.step(10.0, 20.0))
    }

    @Test
    fun `a step before pen-down seeds instead of jumping`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(100.0)
        assertEquals(Pt(3.0, 4.0), s.step(3.0, 4.0))
    }

    @Test
    fun `stabilisation halves a high frequency shake rather than following it`() {
        // A nib jittering either side of a straight run: off, it is written down as it arrives.
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(100.0)
        s.begin(0.0, 0.0)

        var rawWorst = 0.0
        var steadyWorst = 0.0
        var at = 0.0
        for (i in 1..400) {
            val jitter = if (i % 2 == 0) 3.0 else -3.0
            at += 1.5 // the hand walks on along x
            val p = s.step(at, jitter)
            rawWorst = maxOf(rawWorst, abs(jitter))
            // The run's centreline is y = 0, so distance from it is the shake that survived.
            steadyWorst = maxOf(steadyWorst, abs(p.y))
        }
        assertTrue("the raw shake must be visible", rawWorst >= 3.0)
        assertTrue("stabilised shake $steadyWorst must be far below raw $rawWorst", steadyWorst < rawWorst / 2.0)
    }

    @Test
    fun `a straight walk stays straight and reaches where the hand went`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(60.0)
        s.begin(0.0, 0.0)
        // Hold the nib still in y while it travels: the output must never leave y = 0.
        var worstDeviation = 0.0
        for (i in 1..500) {
            val p = s.step(i * 1.0, 0.0)
            worstDeviation = maxOf(worstDeviation, abs(p.y - 0.0))
        }
        assertEquals(0.0, worstDeviation, 0.0)
    }

    @Test
    fun `the output trails the nib by no more than the configured lag`() {
        val lambda = PenStabilizer.lambdaFor(100.0)
        val s = PenStabilizer()
        s.lambda = lambda
        s.begin(0.0, 0.0)
        var at = 0.0
        var worst = 0.0
        for (i in 1..500) {
            at += 2.0
            val p = s.step(at, 0.0)
            worst = maxOf(worst, at - p.x)
        }
        assertTrue("lag $worst must not exceed the configured $lambda", worst <= lambda + 1e-6)
        assertTrue("lag $worst should be felt at all", worst > lambda * 0.5)
    }

    @Test
    fun `a pen that stops mid stroke leaves the path where it was`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(100.0)
        s.begin(0.0, 0.0)
        var p = Pt(0.0, 0.0)
        for (i in 1..50) p = s.step(i * 2.0, 0.0)
        val held = p
        // The nib rests: no travel, so the filter has no arc to move along.
        for (i in 1..50) p = s.step(100.0, 0.0)
        assertEquals(held.x, p.x, 1e-9)
        assertEquals(held.y, p.y, 1e-9)
    }

    @Test
    fun `the lag scales with the setting and is bounded by the slider's top`() {
        assertEquals(0.0, PenStabilizer.lambdaFor(0.0), 1e-9)
        assertEquals(PenStabilizer.MAX_LAMBDA_PX, PenStabilizer.lambdaFor(100.0), 1e-9)
        assertEquals(PenStabilizer.MAX_LAMBDA_PX / 2.0, PenStabilizer.lambdaFor(50.0), 1e-9)
        assertEquals(PenStabilizer.MAX_LAMBDA_PX, PenStabilizer.lambdaFor(1e9), 1e-9)
        assertEquals(0.0, PenStabilizer.lambdaFor(-5.0), 1e-9)
        assertEquals(0.0, PenStabilizer.lambdaFor(Double.NaN), 1e-9)
    }

    @Test
    fun `a nonsensical lag reads as off rather than poisoning the path`() {
        val s = PenStabilizer()
        s.lambda = Double.NaN
        assertEquals(0.0, s.lambda, 0.0)
        s.lambda = -3.0
        assertEquals(0.0, s.lambda, 0.0)
        s.lambda = Double.POSITIVE_INFINITY
        assertEquals(0.0, s.lambda, 0.0)
    }

    @Test
    fun `samples arrive with sub-pixel travel and the filter still moves off the seed`() {
        val s = PenStabilizer()
        s.lambda = PenStabilizer.lambdaFor(100.0)
        s.begin(0.0, 0.0)
        var p = Pt(0.0, 0.0)
        var travelled = 0.0
        while (travelled < 40.0) {
            p = s.step(travelled + 0.25, 0.0)
            travelled += 0.25
        }
        assertTrue("the path must have followed the hand: ${p.x}", p.x > 20.0)
        assertTrue("and must still be behind it", p.x < travelled)
        assertTrue("and never ahead of it", hypot(p.x, p.y) <= travelled + 1e-9)
    }
}