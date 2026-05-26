package com.zibete.driverassistant.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDirectionCalculatorTest {
    private val calculator = HomeDirectionCalculator()
    private val home = HomeLocation(
        latitude = 0.0,
        longitude = 0.1,
        address = "Direccion de casa",
        label = "Casa",
        resolvedAtMillis = 1_700_000_000_000L,
        provider = "test"
    )

    @Test
    fun destinationExactlyTowardHomeScoresHigh() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Camino a casa",
            destinationLocation = destination(latitude = 0.0, longitude = 0.05)
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) >= 9)
        assertEquals("Casa ${result.score}/10", result.shortLabel)
    }

    @Test
    fun destinationNearHomeScoresHigh() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Cerca de casa",
            destinationLocation = destination(latitude = 0.0, longitude = 0.098)
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) >= 9)
    }

    @Test
    fun neutralDestinationScoresMiddle() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Destino lateral",
            destinationLocation = destination(latitude = 0.1, longitude = 0.1)
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) in 4..6)
    }

    @Test
    fun oppositeDirectionScoresLow() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Lejos de casa",
            destinationLocation = destination(latitude = 0.0, longitude = -0.05)
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) <= 2)
    }

    @Test
    fun destinationFartherFromHomeScoresLow() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Mucho mas lejos",
            destinationLocation = destination(latitude = 0.0, longitude = -0.2)
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) <= 2)
        assertTrue(requireNotNull(result.deltaToHomeKm) < 0.0)
    }

    @Test
    fun missingHomeLocationReturnsMissingStatus() {
        val result = calculator.calculate(
            homeLocation = null,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Destino",
            destinationLocation = destination(latitude = 0.0, longitude = 0.05)
        )

        assertUnavailable(HomeDirectionStatus.MISSING_HOME_LOCATION, result)
    }

    @Test
    fun missingCurrentLocationReturnsMissingStatus() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = null,
            currentLongitude = 0.0,
            destinationText = "Destino",
            destinationLocation = destination(latitude = 0.0, longitude = 0.05)
        )

        assertUnavailable(HomeDirectionStatus.MISSING_CURRENT_LOCATION, result)
    }

    @Test
    fun missingDestinationTextReturnsMissingStatus() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = " ",
            destinationLocation = destination(latitude = 0.0, longitude = 0.05)
        )

        assertUnavailable(HomeDirectionStatus.MISSING_DESTINATION_TEXT, result)
    }

    @Test
    fun unresolvedDestinationReturnsMissingStatus() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Destino",
            destinationLocation = null
        )

        assertUnavailable(HomeDirectionStatus.DESTINATION_NOT_RESOLVED, result)
    }

    @Test
    fun lowConfidenceDestinationReturnsMissingStatus() {
        val result = calculator.calculate(
            homeLocation = home,
            currentLatitude = 0.0,
            currentLongitude = 0.0,
            destinationText = "Destino ambiguo",
            destinationLocation = destination(latitude = 0.0, longitude = 0.05, confidence = 0.4)
        )

        assertUnavailable(HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION, result)
    }

    private fun assertUnavailable(
        status: HomeDirectionStatus,
        result: HomeDirectionResult
    ) {
        assertEquals(status, result.status)
        assertNull(result.score)
        assertEquals("Casa ?", result.shortLabel)
    }

    private fun destination(
        latitude: Double,
        longitude: Double,
        confidence: Double? = 0.9
    ): DestinationLocation {
        return DestinationLocation(
            latitude = latitude,
            longitude = longitude,
            sourceText = "Destino",
            confidence = confidence
        )
    }
}
