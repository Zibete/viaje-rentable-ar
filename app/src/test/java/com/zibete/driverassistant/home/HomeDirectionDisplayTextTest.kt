package com.zibete.driverassistant.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDirectionDisplayTextTest {
    @Test
    fun mapsAvailableToScoreLabel() {
        assertEquals(
            "Casa 8/10",
            result(HomeDirectionStatus.AVAILABLE, score = 8, shortLabel = "Casa 8/10").toDiagnosticShortLabel()
        )
    }

    @Test
    fun mapsMissingHomeToShortReason() {
        assertEquals(
            "Casa ? sin domicilio",
            result(HomeDirectionStatus.MISSING_HOME_LOCATION).toDiagnosticShortLabel()
        )
    }

    @Test
    fun mapsMissingCurrentLocationToShortReason() {
        assertEquals(
            "Casa ? sin ubicacion",
            result(HomeDirectionStatus.MISSING_CURRENT_LOCATION).toDiagnosticShortLabel()
        )
    }

    @Test
    fun mapsMissingDestinationTextToShortReason() {
        assertEquals(
            "Casa ? sin destino",
            result(HomeDirectionStatus.MISSING_DESTINATION_TEXT).toDiagnosticShortLabel()
        )
    }

    @Test
    fun mapsDestinationNotResolvedToShortReason() {
        assertEquals(
            "Casa ? geocoder",
            result(HomeDirectionStatus.DESTINATION_NOT_RESOLVED).toDiagnosticShortLabel()
        )
    }

    @Test
    fun mapsLowConfidenceDestinationToShortReason() {
        assertEquals(
            "Casa ? baja confianza",
            result(HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION).toDiagnosticShortLabel()
        )
    }

    private fun result(
        status: HomeDirectionStatus,
        score: Int? = null,
        shortLabel: String = "Casa ?"
    ): HomeDirectionResult {
        return HomeDirectionResult(
            status = status,
            score = score,
            distanceNowToHomeKm = null,
            distanceDestinationToHomeKm = null,
            deltaToHomeKm = null,
            shortLabel = shortLabel
        )
    }
}
