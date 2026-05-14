package com.zibete.driverassistant.ocr

import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger
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
        DriverAssistantDebugLogger.log("pipeline rawText", rawText)
        if (rawText.isNullOrBlank()) {
            DriverAssistantDebugLogger.log("pipeline result", "NoText")
            return TripOfferAnalysisResult.NoText
        }

        val candidate = parser.parse(rawText)
            ?: run {
                DriverAssistantDebugLogger.log("pipeline result", "NoTripDetected")
                return TripOfferAnalysisResult.NoTripDetected
            }
        DriverAssistantDebugLogger.log("pipeline candidate", candidate)

        val input = candidate.toTripOfferInput()
        DriverAssistantDebugLogger.log("pipeline input", input)

        val zoneMatch = zoneMatcher.findMatch(candidate.rawText, config.avoidZones)
        DriverAssistantDebugLogger.log("pipeline zoneMatch", zoneMatch)

        val result = calculator.calculate(
            input = input,
            config = config,
            zoneMatch = zoneMatch
        )
        DriverAssistantDebugLogger.log("pipeline decisionResult", result)

        return TripOfferAnalysisResult.DecisionReady(
            result = result,
            input = input
        )
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
    data class DecisionReady(
        val result: TripDecisionResult,
        val input: TripOfferInput
    ) : TripOfferAnalysisResult
}
