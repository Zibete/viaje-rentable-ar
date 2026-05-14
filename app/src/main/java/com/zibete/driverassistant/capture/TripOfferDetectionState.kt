package com.zibete.driverassistant.capture

import com.zibete.driverassistant.calculator.TripOfferInput

class TripOfferDetectionState {
    private var lastShownSignature: TripOfferSignature? = null

    fun shouldShowOverlay(input: TripOfferInput): Boolean {
        val signature = TripOfferSignature.fromTripOfferInput(input)
        return signature != lastShownSignature
    }

    fun markOverlayShown(input: TripOfferInput) {
        lastShownSignature = TripOfferSignature.fromTripOfferInput(input)
    }

    fun reset() {
        lastShownSignature = null
    }
}
