package com.zibete.driverassistant.ocr

import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger
import com.zibete.driverassistant.zones.AvoidZoneMatcher

class TripOfferDecisionPipeline(
    private val parser: TripOfferTextParser = TripOfferTextParser(),
    private val presenceValidator: TripOfferPresenceValidator = TripOfferPresenceValidator(),
    private val calculator: DriverProfitCalculator = DriverProfitCalculator(),
    private val zoneMatcher: AvoidZoneMatcher = AvoidZoneMatcher()
) {
    fun analyzeRecognizedText(
        rawText: String?,
        config: DriverConfig,
        traceId: String? = null
    ): TripOfferAnalysisResult {
        DriverAssistantDebugLogger.log(
            "pipeline raw text",
            "traceId=${traceId.orNone()}, present=${!rawText.isNullOrBlank()}, textLength=${rawText?.length ?: 0}"
        )
        if (rawText.isNullOrBlank()) {
            DriverAssistantDebugLogger.log("pipeline presence result", "traceId=${traceId.orNone()}, activeOffer=false, reason=NoText")
            return TripOfferAnalysisResult.NoText
        }

        if (!presenceValidator.hasActiveOffer(rawText)) {
            DriverAssistantDebugLogger.log(
                "pipeline presence result",
                "traceId=${traceId.orNone()}, activeOffer=false, reason=no active offer action marker"
            )
            return TripOfferAnalysisResult.NoTripDetected
        }
        DriverAssistantDebugLogger.log("pipeline presence result", "traceId=${traceId.orNone()}, activeOffer=true")

        val candidate = parser.parse(rawText, traceId)
            ?: run {
                DriverAssistantDebugLogger.log("pipeline result", "traceId=${traceId.orNone()}, result=NoTripDetected")
                return TripOfferAnalysisResult.NoTripDetected
            }
        DriverAssistantDebugLogger.log(
            "pipeline candidate summary",
            "traceId=${traceId.orNone()}, fare=${candidate.fareAmount}, pickupKm=${candidate.pickupKm}, " +
                "tripKm=${candidate.tripKm}, pickupMinutes=${candidate.pickupMinutes}, " +
                "tripMinutes=${candidate.tripMinutes}, platform=${candidate.platform}, confidence=${candidate.confidence}"
        )

        val input = candidate.toTripOfferInput()
        DriverAssistantDebugLogger.log(
            "pipeline input summary",
            "traceId=${traceId.orNone()}, fare=${input.fareAmount}, pickupKm=${input.pickupKm}, " +
                "tripKm=${input.tripKm}, pickupMinutes=${input.pickupMinutes}, " +
                "tripMinutes=${input.tripMinutes}, platform=${input.platform}"
        )

        val zoneMatch = zoneMatcher.findMatch(candidate.rawText, config.avoidZones)
        DriverAssistantDebugLogger.log("pipeline zone match", "traceId=${traceId.orNone()}, zoneMatch=$zoneMatch")

        val result = calculator.calculate(
            input = input,
            config = config,
            zoneMatch = zoneMatch
        )
        DriverAssistantDebugLogger.log(
            "pipeline decision result summary",
            "traceId=${traceId.orNone()}, decision=${result.decision}, fare=${result.fareAmount}, " +
                "arsPerKm=${result.arsPerKm}, arsPerHour=${result.arsPerHour}, totalKm=${result.totalKm}, " +
                "totalMinutes=${result.totalMinutes}, rejectionReasons=${result.rejectionReasons}, " +
                "reviewReasons=${result.reviewReasons}"
        )

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

    private fun String?.orNone(): String = this ?: "none"
}

sealed interface TripOfferAnalysisResult {
    data object NoText : TripOfferAnalysisResult
    data object NoTripDetected : TripOfferAnalysisResult
    data class DecisionReady(
        val result: TripDecisionResult,
        val input: TripOfferInput
    ) : TripOfferAnalysisResult
}
