package com.zibete.driverassistant.capture

import com.zibete.driverassistant.calculator.TripOfferInput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDirectionScoreUpdateStateTest {
    @Test
    fun appliesOnlyActiveOfferKey() {
        val state = HomeDirectionScoreUpdateState()
        val firstKey = key(destinationText = "Pilar")
        val secondKey = key(destinationText = "San Miguel")

        state.markActive(firstKey)
        state.markActive(secondKey)

        assertFalse(state.shouldApply(firstKey))
        assertTrue(state.shouldApply(secondKey))
    }

    @Test
    fun immediateScoreForActiveOfferKeyIsApplied() {
        val state = HomeDirectionScoreUpdateState()
        val key = key(destinationText = "Pilar")

        state.markActive(key)

        assertTrue(state.shouldApply(key))
    }

    @Test
    fun clearDiscardsPendingKeys() {
        val state = HomeDirectionScoreUpdateState()
        val key = key(destinationText = "Pilar")

        state.markActive(key)
        state.clear()

        assertFalse(state.shouldApply(key))
    }

    private fun key(destinationText: String): TripOfferSignature {
        return TripOfferSignature.fromTripOfferInput(
            TripOfferInput(
                fareAmount = 7500.0,
                pickupKm = 1.0,
                tripKm = 7.0,
                pickupMinutes = 5.0,
                tripMinutes = 35.0,
                platform = "uber",
                destinationText = destinationText,
                rawText = "raw"
            )
        )
    }
}
