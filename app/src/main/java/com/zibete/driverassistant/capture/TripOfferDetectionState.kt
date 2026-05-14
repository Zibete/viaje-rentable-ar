package com.zibete.driverassistant.capture

import com.zibete.driverassistant.calculator.TripDecisionResult

class TripOfferDetectionState {
    private var lastShownSignature: TripOfferSignature? = null

    fun shouldShowOverlay(
        result: TripDecisionResult,
        rawText: String?
    ): Boolean {
        val signature = TripOfferSignature.fromDecisionResult(
            result = result,
            rawText = rawText
        )
        if (signature == lastShownSignature) {
            return false
        }

        lastShownSignature = signature
        return true
    }

    fun reset() {
        lastShownSignature = null
    }
}
