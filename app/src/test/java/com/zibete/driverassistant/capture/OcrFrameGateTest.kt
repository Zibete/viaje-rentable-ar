package com.zibete.driverassistant.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrFrameGateTest {
    @Test
    fun allowsFirstFrame() {
        val gate = OcrFrameGate()

        val decision = gate.evaluate(signature(10), nowMillis = 0L)

        assertTrue(decision.shouldRunOcr)
    }

    @Test
    fun skipsSimilarFrameAfterRecentOcr() {
        val gate = OcrFrameGate()

        gate.evaluate(signature(10), nowMillis = 0L)
        val decision = gate.evaluate(signature(11), nowMillis = 1_000L)

        assertFalse(decision.shouldRunOcr)
    }

    @Test
    fun allowsChangedFrameImmediatelyWithDefaultGate() {
        val gate = OcrFrameGate()

        gate.evaluate(signature(10), nowMillis = 0L)
        val decision = gate.evaluate(signature(90), nowMillis = 11_000L)

        assertTrue(decision.shouldRunOcr)
    }

    @Test
    fun canWaitForStableCandidateWhenConfigured() {
        val gate = OcrFrameGate(stableFramesRequired = 2)

        gate.evaluate(signature(10), nowMillis = 0L)
        val firstChangedFrame = gate.evaluate(signature(90), nowMillis = 11_000L)
        val stableChangedFrame = gate.evaluate(signature(91), nowMillis = 11_350L)

        assertFalse(firstChangedFrame.shouldRunOcr)
        assertTrue(stableChangedFrame.shouldRunOcr)
    }

    @Test
    fun respectsCooldownForNonStrongChanges() {
        val gate = OcrFrameGate(
            visualChangeThreshold = 10.0,
            strongVisualChangeThreshold = 80.0,
            stableFramesRequired = 1,
            postOcrCooldownMillis = 10_000L
        )

        gate.evaluate(signature(10), nowMillis = 0L)
        val decision = gate.evaluate(signature(45), nowMillis = 1_000L)

        assertFalse(decision.shouldRunOcr)
    }

    @Test
    fun allowsPeriodicSafetyScan() {
        val gate = OcrFrameGate(safetyScanIntervalMillis = 5_000L)

        gate.evaluate(signature(10), nowMillis = 0L)
        val decision = gate.evaluate(signature(10), nowMillis = 5_000L)

        assertTrue(decision.shouldRunOcr)
    }

    private fun signature(value: Int): FrameSignature {
        return FrameSignature(List(16) { value })
    }
}
