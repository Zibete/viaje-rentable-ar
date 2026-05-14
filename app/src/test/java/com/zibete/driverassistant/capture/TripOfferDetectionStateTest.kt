package com.zibete.driverassistant.capture

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripOfferDetectionStateTest {
    @Test
    fun signatureDoesNotChangeForSameOffer() {
        val first = TripOfferSignature.fromDecisionResult(
            result = decisionResult(),
            rawText = "Uber ARS 5.127 41 min 8.0 km"
        )
        val second = TripOfferSignature.fromDecisionResult(
            result = decisionResult(),
            rawText = "uber   ars 5.127 41 min 8.0 km "
        )

        assertEquals(first, second)
    }

    @Test
    fun signatureChangesWhenFareChanges() {
        val first = TripOfferSignature.fromDecisionResult(
            result = decisionResult(fareAmount = 5127.0),
            rawText = "Uber ARS 5.127 41 min 8.0 km"
        )
        val second = TripOfferSignature.fromDecisionResult(
            result = decisionResult(fareAmount = 6127.0),
            rawText = "Uber ARS 6.127 41 min 8.0 km"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun signatureChangesWhenDistanceChanges() {
        val first = TripOfferSignature.fromDecisionResult(
            result = decisionResult(totalKm = 8.0),
            rawText = "Uber ARS 5.127 41 min 8.0 km"
        )
        val second = TripOfferSignature.fromDecisionResult(
            result = decisionResult(totalKm = 9.0),
            rawText = "Uber ARS 5.127 41 min 9.0 km"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun signatureChangesWhenMinutesChange() {
        val first = TripOfferSignature.fromDecisionResult(
            result = decisionResult(totalMinutes = 41.0),
            rawText = "Uber ARS 5.127 41 min 8.0 km"
        )
        val second = TripOfferSignature.fromDecisionResult(
            result = decisionResult(totalMinutes = 45.0),
            rawText = "Uber ARS 5.127 45 min 8.0 km"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun signatureChangesWhenRelevantTextChanges() {
        val first = TripOfferSignature.fromDecisionResult(
            result = decisionResult(),
            rawText = "Uber ARS 5.127 41 min 8.0 km Palermo"
        )
        val second = TripOfferSignature.fromDecisionResult(
            result = decisionResult(),
            rawText = "Uber ARS 5.127 41 min 8.0 km Retiro"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun detectionStateSkipsDuplicateOverlayForSameOffer() {
        val state = TripOfferDetectionState()
        val result = decisionResult()

        assertTrue(state.shouldShowOverlay(result, "Uber ARS 5.127 41 min 8.0 km"))
        assertFalse(state.shouldShowOverlay(result, "uber   ars 5.127 41 min 8.0 km "))
    }

    @Test
    fun detectionStateAllowsOverlayAfterOfferChanges() {
        val state = TripOfferDetectionState()

        assertTrue(state.shouldShowOverlay(decisionResult(), "Uber ARS 5.127 41 min 8.0 km"))
        assertTrue(
            state.shouldShowOverlay(
                decisionResult(fareAmount = 6127.0),
                "Uber ARS 6.127 41 min 8.0 km"
            )
        )
    }

    @Test
    fun detectionStateAllowsSameOfferAfterReset() {
        val state = TripOfferDetectionState()
        val result = decisionResult()

        assertTrue(state.shouldShowOverlay(result, "Uber ARS 5.127 41 min 8.0 km"))
        state.reset()
        assertTrue(state.shouldShowOverlay(result, "Uber ARS 5.127 41 min 8.0 km"))
    }

    private fun decisionResult(
        fareAmount: Double = 5127.0,
        totalKm: Double = 8.0,
        totalMinutes: Double = 41.0
    ): TripDecisionResult {
        return TripDecisionResult(
            decision = DriverDecision.ACCEPT,
            fareAmount = fareAmount,
            arsPerKm = fareAmount / totalKm,
            arsPerHour = fareAmount / (totalMinutes / 60.0),
            estimatedCost = 3470.0,
            estimatedNetProfit = fareAmount - 3470.0,
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            rejectionReasons = emptyList(),
            reviewReasons = emptyList()
        )
    }
}
