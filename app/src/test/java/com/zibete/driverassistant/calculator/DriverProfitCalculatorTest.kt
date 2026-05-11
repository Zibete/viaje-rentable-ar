package com.zibete.driverassistant.calculator

import com.zibete.driverassistant.config.DriverConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverProfitCalculatorTest {
    private val calculator = DriverProfitCalculator()
    private val config = DriverConfig.default()

    @Test
    fun acceptsProfitableTrip() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 1.0,
                tripKm = 7.0,
                pickupMinutes = 5.0,
                tripMinutes = 35.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
    }

    @Test
    fun rejectsTripWithLowArsPerKm() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 4000.0,
                pickupKm = 1.0,
                tripKm = 7.0,
                pickupMinutes = 5.0,
                tripMinutes = 35.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("$/km") })
    }

    @Test
    fun rejectsTripWithLowArsPerHour() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 7000.0,
                pickupKm = 1.0,
                tripKm = 7.0,
                pickupMinutes = 5.0,
                tripMinutes = 75.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("$/hora") })
    }

    @Test
    fun rejectsTripWithLowNetProfit() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 1500.0,
                pickupKm = 1.0,
                tripKm = 1.0,
                pickupMinutes = 5.0,
                tripMinutes = 5.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("Ganancia neta") })
    }

    @Test
    fun rejectsTripWhenPickupKmIsTooHigh() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 4.0,
                tripKm = 1.0,
                pickupMinutes = 5.0,
                tripMinutes = 10.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("Distancia al pasajero") })
    }

    @Test
    fun rejectsTripWhenPickupMinutesIsTooHigh() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 1.0,
                tripKm = 4.0,
                pickupMinutes = 11.0,
                tripMinutes = 5.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("Tiempo al pasajero") })
    }

    @Test
    fun marksReviewWhenMetricIsInsideTolerance() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 5700.0,
                pickupKm = 1.0,
                tripKm = 9.0,
                pickupMinutes = 5.0,
                tripMinutes = 35.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REVIEW, result.decision)
        assertTrue(result.reviewReasons.any { it.contains("tolerancia") })
    }

    @Test
    fun calculatesTotalKm() {
        val result = calculateReferenceTrip()

        assertEquals(8.0, result.totalKm ?: 0.0, 0.001)
    }

    @Test
    fun calculatesTotalMinutes() {
        val result = calculateReferenceTrip()

        assertEquals(41.0, result.totalMinutes ?: 0.0, 0.001)
    }

    @Test
    fun calculatesArsPerKm() {
        val result = calculateReferenceTrip()

        assertEquals(640.875, result.arsPerKm ?: 0.0, 0.001)
    }

    @Test
    fun calculatesArsPerHour() {
        val result = calculateReferenceTrip()

        assertEquals(7502.927, result.arsPerHour ?: 0.0, 0.001)
    }

    @Test
    fun calculatesEstimatedCost() {
        val result = calculateReferenceTrip()

        assertEquals(3470.0, result.estimatedCost ?: 0.0, 0.001)
    }

    @Test
    fun calculatesEstimatedNetProfit() {
        val result = calculateReferenceTrip()

        assertEquals(1657.0, result.estimatedNetProfit ?: 0.0, 0.001)
    }

    private fun calculateReferenceTrip(): TripDecisionResult {
        return calculator.calculate(
            input = TripOfferInput(
                fareAmount = 5127.0,
                pickupKm = 1.0,
                tripKm = 7.0,
                pickupMinutes = 5.0,
                tripMinutes = 36.0,
                platform = "uber"
            ),
            config = config
        )
    }
}

