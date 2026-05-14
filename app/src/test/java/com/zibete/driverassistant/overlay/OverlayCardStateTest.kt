package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayCardStateTest {
    @Test
    fun fromDecisionResultShowsPlaceholdersForNullValues() {
        val state = OverlayCardState.fromDecisionResult(
            TripDecisionResult(
                decision = DriverDecision.REVIEW,
                fareAmount = null,
                arsPerKm = null,
                arsPerHour = null,
                estimatedCost = null,
                estimatedNetProfit = null,
                totalKm = null,
                totalMinutes = null,
                rejectionReasons = emptyList(),
                reviewReasons = listOf("Datos incompletos")
            )
        )

        assertEquals("$ -", state.fareText)
        assertEquals("$ -", state.arsPerKmText)
        assertEquals("$ -", state.arsPerHourText)
        assertEquals("- min", state.totalTimeText)
        assertEquals("- km", state.totalKmText)
    }

    @Test
    fun fromDecisionResultShowsIncompleteDataReasonWhenNoReasonExists() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = null,
                totalKm = null,
                totalMinutes = null,
                arsPerKm = null,
                arsPerHour = null
            )
        )

        assertEquals("Datos incompletos", state.shortReason)
    }

    @Test
    fun completeOverlayDataRequiresAllDisplayedMetrics() {
        assertTrue(
            decisionResult(
                fareAmount = 7124.0,
                totalKm = 14.7,
                totalMinutes = 30.0,
                arsPerKm = 484.6,
                arsPerHour = 14248.0
            ).hasCompleteOverlayData()
        )
        assertFalse(
            decisionResult(
                fareAmount = 7124.0,
                totalKm = null,
                totalMinutes = 30.0,
                arsPerKm = null,
                arsPerHour = 14248.0
            ).hasCompleteOverlayData()
        )
    }

    private fun decisionResult(
        fareAmount: Double?,
        totalKm: Double?,
        totalMinutes: Double?,
        arsPerKm: Double?,
        arsPerHour: Double?
    ): TripDecisionResult {
        return TripDecisionResult(
            decision = DriverDecision.REJECT,
            fareAmount = fareAmount,
            arsPerKm = arsPerKm,
            arsPerHour = arsPerHour,
            estimatedCost = null,
            estimatedNetProfit = null,
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            rejectionReasons = emptyList(),
            reviewReasons = emptyList()
        )
    }
}
