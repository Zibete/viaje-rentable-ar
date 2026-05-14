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
}
