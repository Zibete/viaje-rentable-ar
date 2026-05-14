package com.zibete.driverassistant.ocr

import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.zones.AvoidZoneMatcher

class TripOfferDecisionPipeline(
    private val parser: TripOfferTextParser = TripOfferTextParser(),
    private val calculator: DriverProfitCalculator = DriverProfitCalculator(),
    private val zoneMatcher: AvoidZoneMatcher = AvoidZoneMatcher()
) {
    fun analyzeRecognizedText(
        rawText: String?,
        config: DriverConfig
    ): TripOfferAnalysisResult {
        if (rawText.isNullOrBlank()) {
            return TripOfferAnalysisResult.NoText
        }

        val candidate = parser.parse(rawText)
            ?: return TripOfferAnalysisResult.NoTripDetected
        val input = candidate.toTripOfferInput()
        val zoneMatch = zoneMatcher.findMatch(rawText, config.avoidZones)
        val result = calculator.calculate(
            input = input,
            config = config,
            zoneMatch = zoneMatch
        )

        return TripOfferAnalysisResult.DecisionReady(result)
    }

    private fun TripOfferCandidate.toTripOfferInput(): TripOfferInput {
        return TripOfferInput(
            fareAmount = fareAmount,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMinutes,
            tripMinutes = tripMinutes,
            platform = platform,
            rawText = rawText
        )
    }
}

sealed interface TripOfferAnalysisResult {
    data object NoText : TripOfferAnalysisResult
    data object NoTripDetected : TripOfferAnalysisResult
    data class DecisionReady(val result: TripDecisionResult) : TripOfferAnalysisResult
}
