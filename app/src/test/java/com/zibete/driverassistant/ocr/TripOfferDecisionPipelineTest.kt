package com.zibete.driverassistant.ocr

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.config.DriverConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripOfferDecisionPipelineTest {
    private val pipeline = TripOfferDecisionPipeline()
    private val config = DriverConfig.default()

    @Test
    fun calculatesDecisionFromRecognizedText() {
        val result = pipeline.analyzeRecognizedText(
            rawText = "Uber ARS 10.000 5 min 1 km 35 min 7 km",
            config = config
        )

        val decision = result as TripOfferAnalysisResult.DecisionReady
        assertEquals(DriverDecision.ACCEPT, decision.result.decision)
        assertEquals(10000.0, decision.result.fareAmount ?: 0.0, 0.001)
        assertEquals(8.0, decision.result.totalKm ?: 0.0, 0.001)
        assertEquals(40.0, decision.result.totalMinutes ?: 0.0, 0.001)
    }

    @Test
    fun returnsNoTextWhenRecognizedTextIsBlank() {
        val result = pipeline.analyzeRecognizedText(
            rawText = "   ",
            config = config
        )

        assertEquals(TripOfferAnalysisResult.NoText, result)
    }

    @Test
    fun returnsNoTripDetectedWhenParserFindsNoOfferFields() {
        val result = pipeline.analyzeRecognizedText(
            rawText = "Pantalla sin datos de viaje",
            config = config
        )

        assertEquals(TripOfferAnalysisResult.NoTripDetected, result)
    }

    @Test
    fun letsCalculatorReviewPartialParsedData() {
        val result = pipeline.analyzeRecognizedText(
            rawText = "Uber ARS 5.127 8 km",
            config = config
        )

        val decision = result as TripOfferAnalysisResult.DecisionReady
        assertEquals(DriverDecision.REVIEW, decision.result.decision)
        assertTrue(decision.result.reviewReasons.any { it.contains("incompleto") })
    }

    @Test
    fun calculatesDecisionFromUberOfferWhenOwnOverlayTextIsPresent() {
        val result = pipeline.analyzeRecognizedText(
            rawText = """
                RECHAZAR
                ${'$'} 10
                ${'$'} 5/h - ${'$'} 0/km
                131,5 min - 44,1 km
                ${'$'}/km por debajo del minimo

                UberX
                7.124 ARS
                A 6 min (2.5 km) de distancia
                Viaje de 24 min (12.2 km)
            """.trimIndent(),
            config = config
        )

        val decision = result as TripOfferAnalysisResult.DecisionReady
        assertEquals(DriverDecision.REJECT, decision.result.decision)
        assertEquals(7124.0, decision.result.fareAmount ?: 0.0, 0.001)
        assertEquals(14.7, decision.result.totalKm ?: 0.0, 0.001)
        assertEquals(30.0, decision.result.totalMinutes ?: 0.0, 0.001)
        assertEquals(484.626, decision.result.arsPerKm ?: 0.0, 0.001)
        assertEquals(14248.0, decision.result.arsPerHour ?: 0.0, 0.001)
        assertTrue(decision.result.rejectionReasons.any { it.contains("$/km") })
    }

    @Test
    fun calculatesDecisionFromUberOfferIgnoringManyExtraNumbers() {
        val result = pipeline.analyzeRecognizedText(
            rawText = """
                RECHAZAR
                ${'$'} 10
                ${'$'} 5/h - ${'$'} 0/km
                131,5 min - 44,1 km
                4,98 (253)
                14:35
                87%
                UberX
                7.124 ARS
                A 6 min (2.5 km) de distancia
                Viaje de 24 min (12.2 km)
                453.2 km
                56.4 km
            """.trimIndent(),
            config = config
        )

        val decision = result as TripOfferAnalysisResult.DecisionReady
        assertEquals(DriverDecision.REJECT, decision.result.decision)
        assertEquals(7124.0, decision.result.fareAmount ?: 0.0, 0.001)
        assertEquals(14.7, decision.result.totalKm ?: 0.0, 0.001)
        assertEquals(30.0, decision.result.totalMinutes ?: 0.0, 0.001)
        assertEquals(484.626, decision.result.arsPerKm ?: 0.0, 0.001)
        assertEquals(14248.0, decision.result.arsPerHour ?: 0.0, 0.001)
        assertTrue(decision.result.rejectionReasons.any { it.contains("$/km") })
    }
}
