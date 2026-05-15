package com.zibete.driverassistant.capture

import kotlin.math.abs

data class FrameSignature(
    val cells: List<Int>
) {
    fun differenceScore(other: FrameSignature): Double {
        if (cells.isEmpty() || other.cells.isEmpty()) return Double.MAX_VALUE
        if (cells.size != other.cells.size) return Double.MAX_VALUE

        return cells.indices
            .sumOf { index -> abs(cells[index] - other.cells[index]) }
            .toDouble() / cells.size
    }
}

data class OcrFrameGateDecision(
    val shouldRunOcr: Boolean,
    val reason: String,
    val changeScore: Double? = null,
    val nextFrameDelayMillis: Long
)

class OcrFrameGate(
    private val visualChangeThreshold: Double = DEFAULT_VISUAL_CHANGE_THRESHOLD,
    private val strongVisualChangeThreshold: Double = DEFAULT_STRONG_VISUAL_CHANGE_THRESHOLD,
    private val stabilityChangeThreshold: Double = DEFAULT_STABILITY_CHANGE_THRESHOLD,
    private val stableFramesRequired: Int = DEFAULT_STABLE_FRAMES_REQUIRED,
    private val postOcrCooldownMillis: Long = DEFAULT_POST_OCR_COOLDOWN_MILLIS,
    private val safetyScanIntervalMillis: Long = DEFAULT_SAFETY_SCAN_INTERVAL_MILLIS,
    private val idleFrameDelayMillis: Long = DEFAULT_IDLE_FRAME_DELAY_MILLIS,
    private val candidateFrameDelayMillis: Long = DEFAULT_CANDIDATE_FRAME_DELAY_MILLIS,
    private val cooldownFrameDelayMillis: Long = DEFAULT_COOLDOWN_FRAME_DELAY_MILLIS
) {
    private var lastFrameSignature: FrameSignature? = null
    private var candidateSignature: FrameSignature? = null
    private var stableCandidateFrames: Int = 0
    private var lastOcrAtMillis: Long? = null

    fun evaluate(
        signature: FrameSignature,
        nowMillis: Long
    ): OcrFrameGateDecision {
        val previousSignature = lastFrameSignature
        if (previousSignature == null) {
            lastFrameSignature = signature
            return allowOcr(
                nowMillis = nowMillis,
                reason = "first frame",
                changeScore = null
            )
        }

        val changeScore = signature.differenceScore(previousSignature)
        lastFrameSignature = signature

        candidateSignature?.let { candidate ->
            if (signature.differenceScore(candidate) <= stabilityChangeThreshold) {
                stableCandidateFrames += 1
                return if (stableCandidateFrames >= stableFramesRequired) {
                    allowOcr(
                        nowMillis = nowMillis,
                        reason = "stable visual candidate",
                        changeScore = changeScore
                    )
                } else {
                    skipOcr(
                        reason = "waiting stable candidate",
                        changeScore = changeScore,
                        nextFrameDelayMillis = candidateFrameDelayMillis
                    )
                }
            }
        }

        if (changeScore < visualChangeThreshold) {
            resetCandidate()
            return if (shouldRunSafetyScan(nowMillis)) {
                allowOcr(
                    nowMillis = nowMillis,
                    reason = "periodic safety scan",
                    changeScore = changeScore
                )
            } else {
                skipOcr(
                    reason = "stable frame",
                    changeScore = changeScore,
                    nextFrameDelayMillis = idleFrameDelayMillis
                )
            }
        }

        if (isInCooldown(nowMillis) && changeScore < strongVisualChangeThreshold) {
            resetCandidate()
            return skipOcr(
                reason = "cooldown",
                changeScore = changeScore,
                nextFrameDelayMillis = cooldownFrameDelayMillis
            )
        }

        candidateSignature = signature
        stableCandidateFrames = 1
        return skipOcr(
            reason = "waiting stable candidate",
            changeScore = changeScore,
            nextFrameDelayMillis = candidateFrameDelayMillis
        )
    }

    fun reset() {
        lastFrameSignature = null
        resetCandidate()
        lastOcrAtMillis = null
    }

    private fun allowOcr(
        nowMillis: Long,
        reason: String,
        changeScore: Double?
    ): OcrFrameGateDecision {
        lastOcrAtMillis = nowMillis
        resetCandidate()
        return OcrFrameGateDecision(
            shouldRunOcr = true,
            reason = reason,
            changeScore = changeScore,
            nextFrameDelayMillis = candidateFrameDelayMillis
        )
    }

    private fun skipOcr(
        reason: String,
        changeScore: Double?,
        nextFrameDelayMillis: Long
    ): OcrFrameGateDecision {
        return OcrFrameGateDecision(
            shouldRunOcr = false,
            reason = reason,
            changeScore = changeScore,
            nextFrameDelayMillis = nextFrameDelayMillis
        )
    }

    private fun shouldRunSafetyScan(nowMillis: Long): Boolean {
        val lastOcr = lastOcrAtMillis ?: return true
        return nowMillis - lastOcr >= safetyScanIntervalMillis
    }

    private fun isInCooldown(nowMillis: Long): Boolean {
        val lastOcr = lastOcrAtMillis ?: return false
        return nowMillis - lastOcr < postOcrCooldownMillis
    }

    private fun resetCandidate() {
        candidateSignature = null
        stableCandidateFrames = 0
    }

    private companion object {
        private const val DEFAULT_VISUAL_CHANGE_THRESHOLD = 18.0
        private const val DEFAULT_STRONG_VISUAL_CHANGE_THRESHOLD = 42.0
        private const val DEFAULT_STABILITY_CHANGE_THRESHOLD = 8.0
        private const val DEFAULT_STABLE_FRAMES_REQUIRED = 2
        private const val DEFAULT_POST_OCR_COOLDOWN_MILLIS = 10_000L
        private const val DEFAULT_SAFETY_SCAN_INTERVAL_MILLIS = 30_000L
        private const val DEFAULT_IDLE_FRAME_DELAY_MILLIS = 2_500L
        private const val DEFAULT_CANDIDATE_FRAME_DELAY_MILLIS = 800L
        private const val DEFAULT_COOLDOWN_FRAME_DELAY_MILLIS = 1_500L
    }
}
