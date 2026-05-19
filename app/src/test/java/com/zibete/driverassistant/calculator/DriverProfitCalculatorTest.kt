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
    fun acceptsProfitableTripWhenPlatformIsNull() {
        val result = calculator.calculate(
            input = profitableInput(platform = null),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
        assertTrue(result.reviewReasons.none { it.contains("Plataforma") })
    }

    @Test
    fun acceptsProfitableTripWhenPlatformIsUnknown() {
        val result = calculator.calculate(
            input = profitableInput(platform = "UNKNOWN"),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
        assertTrue(result.reviewReasons.none { it.contains("Plataforma") })
    }

    @Test
    fun acceptsProfitableTripWhenPlatformWouldPreviouslyBeDisabled() {
        val result = calculator.calculate(
            input = profitableInput(platform = "didi"),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
        assertTrue(result.reviewReasons.none { it.contains("Plataforma") })
    }

    @Test
    fun acceptsProfitableTripWithUnknownPlatform() {
        val result = calculator.calculate(
            input = profitableInput(platform = "new-platform"),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
    }

    @Test
    fun rejectsUnprofitableTripWithUnknownPlatformByProfitability() {
        val result = calculator.calculate(
            input = profitableInput(
                fareAmount = 4000.0,
                platform = "new-platform"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("$/km") })
        assertTrue(result.reviewReasons.none { it.contains("Plataforma") })
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
    fun acceptsTripWithHighPickupKmWhenTotalProfitabilityPasses() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 20000.0,
                pickupKm = 20.0,
                tripKm = 5.0,
                pickupMinutes = 5.0,
                tripMinutes = 15.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
        assertEquals(25.0, result.totalKm ?: 0.0, 0.001)
    }

    @Test
    fun acceptsTripWithHighPickupMinutesWhenTotalProfitabilityPasses() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 1.0,
                tripKm = 2.0,
                pickupMinutes = 60.0,
                tripMinutes = 10.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.ACCEPT, result.decision)
        assertEquals(70.0, result.totalMinutes ?: 0.0, 0.001)
    }

    @Test
    fun rejectsTripWhenHighPickupMakesTotalArsPerKmTooLow() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 20.0,
                tripKm = 1.0,
                pickupMinutes = 5.0,
                tripMinutes = 15.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("$/km") })
    }

    @Test
    fun rejectsTripWhenHighPickupMakesTotalArsPerHourTooLow() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 1.0,
                tripKm = 1.0,
                pickupMinutes = 100.0,
                tripMinutes = 5.0,
                platform = "uber"
            ),
            config = config
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("$/hora") })
    }

    @Test
    fun rejectsTripWhenHighPickupLeavesNetProfitBelowMinimum() {
        val result = calculator.calculate(
            input = TripOfferInput(
                fareAmount = 10000.0,
                pickupKm = 20.0,
                tripKm = 1.0,
                pickupMinutes = 5.0,
                tripMinutes = 5.0,
                platform = "uber"
            ),
            config = config.copy(
                minArsPerKm = 100.0,
                minArsPerHour = 100.0,
                minNetProfit = 5000.0
            )
        )

        assertEquals(DriverDecision.REJECT, result.decision)
        assertTrue(result.rejectionReasons.any { it.contains("Ganancia neta") })
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
    fun marksReviewWhenLowArsPerKmIsRescuedByHighArsPerHourAndNetProfit() {
        listOf(
            ReviewRescueCase(fareAmount = 4229.0, totalKm = 7.9, totalMinutes = 18.0),
            ReviewRescueCase(fareAmount = 4076.0, totalKm = 9.1, totalMinutes = 11.0),
            ReviewRescueCase(fareAmount = 7124.0, totalKm = 14.7, totalMinutes = 30.0)
        ).forEach { testCase ->
            val result = calculator.calculate(
                input = inputFromTotals(
                    fareAmount = testCase.fareAmount,
                    totalKm = testCase.totalKm,
                    totalMinutes = testCase.totalMinutes
                ),
                config = config
            )

            assertEquals(testCase.toString(), DriverDecision.REVIEW, result.decision)
            assertTrue(
                testCase.toString(),
                result.reviewReasons.contains("$/km bajo, pero $/h alto: revisar destino")
            )
            assertTrue(testCase.toString(), result.rejectionReasons.isEmpty())
        }
    }

    @Test
    fun rejectsTripsWithVeryLowArsPerKmEvenWhenArsPerHourIsHigh() {
        listOf(
            ReviewRescueCase(fareAmount = 16437.0, totalKm = 43.9, totalMinutes = 54.0),
            ReviewRescueCase(fareAmount = 6205.0, totalKm = 17.9, totalMinutes = 30.0)
        ).forEach { testCase ->
            val result = calculator.calculate(
                input = inputFromTotals(
                    fareAmount = testCase.fareAmount,
                    totalKm = testCase.totalKm,
                    totalMinutes = testCase.totalMinutes
                ),
                config = config
            )

            assertEquals(testCase.toString(), DriverDecision.REJECT, result.decision)
            assertTrue(
                testCase.toString(),
                result.rejectionReasons.contains("$/km muy bajo para el viaje")
            )
        }
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

    private fun profitableInput(
        fareAmount: Double = 10000.0,
        platform: String?
    ): TripOfferInput {
        return TripOfferInput(
            fareAmount = fareAmount,
            pickupKm = 1.0,
            tripKm = 7.0,
            pickupMinutes = 5.0,
            tripMinutes = 35.0,
            platform = platform
        )
    }

    private fun inputFromTotals(
        fareAmount: Double,
        totalKm: Double,
        totalMinutes: Double
    ): TripOfferInput {
        return TripOfferInput(
            fareAmount = fareAmount,
            pickupKm = 0.0,
            tripKm = totalKm,
            pickupMinutes = 0.0,
            tripMinutes = totalMinutes,
            platform = "uber"
        )
    }

    private data class ReviewRescueCase(
        val fareAmount: Double,
        val totalKm: Double,
        val totalMinutes: Double
    )
}
