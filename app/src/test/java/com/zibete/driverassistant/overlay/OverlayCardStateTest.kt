package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.home.HomeDirectionResult
import com.zibete.driverassistant.home.HomeDirectionStatus
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
        assertEquals(OverlayVisualState.INCOMPLETE_DATA, state.visualState)
        assertEquals("DATOS INCOMPLETOS", state.titleText)
    }

    @Test
    fun fromDecisionResultUsesIncompleteVisualStateWhenFareIsMissing() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = null,
                totalKm = null,
                totalMinutes = null,
                arsPerKm = null,
                arsPerHour = null
            )
        )

        assertEquals(DriverDecision.REJECT, state.decision)
        assertEquals(OverlayVisualState.INCOMPLETE_DATA, state.visualState)
        assertEquals("DATOS INCOMPLETOS", state.titleText)
        assertEquals("Falta tarifa", state.shortReason)
    }

    @Test
    fun fromDecisionResultUsesDistanceMissingReasonBeforeGenericIncompleteReason() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = 7124.0,
                totalKm = null,
                totalMinutes = 30.0,
                arsPerKm = null,
                arsPerHour = 14248.0
            )
        )

        assertEquals(OverlayVisualState.INCOMPLETE_DATA, state.visualState)
        assertEquals("Falta distancia", state.shortReason)
    }

    @Test
    fun fromDecisionResultUsesTimeMissingReasonWhenOnlyTimeDataIsMissing() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = 7124.0,
                totalKm = 14.7,
                totalMinutes = null,
                arsPerKm = 484.6,
                arsPerHour = null
            )
        )

        assertEquals(OverlayVisualState.INCOMPLETE_DATA, state.visualState)
        assertEquals("Falta tiempo", state.shortReason)
    }

    @Test
    fun fromDecisionResultKeepsDecisionVisualStateForCompleteData() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = 7124.0,
                totalKm = 14.7,
                totalMinutes = 30.0,
                arsPerKm = 484.6,
                arsPerHour = 14248.0
            )
        )

        assertEquals(OverlayVisualState.DECISION, state.visualState)
        assertEquals("RECHAZAR", state.titleText)
        assertEquals(DriverDecision.REJECT, state.decision)
        assertEquals(null, state.shortReason)
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

    @Test
    fun fromDecisionResultAddsHomeDirectionTextWhenAvailable() {
        val state = OverlayCardState.fromDecisionResult(
            result = decisionResult(
                fareAmount = 7500.0,
                totalKm = 10.0,
                totalMinutes = 50.0,
                arsPerKm = 750.0,
                arsPerHour = 9000.0
            ),
            homeDirectionResult = HomeDirectionResult(
                status = HomeDirectionStatus.AVAILABLE,
                score = 8,
                distanceNowToHomeKm = 12.0,
                distanceDestinationToHomeKm = 4.0,
                deltaToHomeKm = 8.0,
                shortLabel = "Casa 8/10"
            )
        )

        assertEquals("Casa 8/10", state.homeDirectionText)
    }

    @Test
    fun fromDecisionResultUsesUnknownHomeDirectionByDefault() {
        val state = OverlayCardState.fromDecisionResult(
            decisionResult(
                fareAmount = 7500.0,
                totalKm = 10.0,
                totalMinutes = 50.0,
                arsPerKm = 750.0,
                arsPerHour = 9000.0
            )
        )

        assertEquals("Casa ?", state.homeDirectionText)
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
