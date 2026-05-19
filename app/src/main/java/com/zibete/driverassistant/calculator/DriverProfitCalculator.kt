package com.zibete.driverassistant.calculator

import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.zones.AvoidZonePolicy
import com.zibete.driverassistant.zones.ZoneMatchResult

class DriverProfitCalculator {
    fun calculate(
        input: TripOfferInput,
        config: DriverConfig,
        zoneMatch: ZoneMatchResult? = null,
        isOcrAmbiguous: Boolean = false
    ): TripDecisionResult {
        val rejectionReasons = mutableListOf<String>()
        val reviewReasons = mutableListOf<String>()
        var hasCriticalMissingData = false

        if (input.fareAmount == null) {
            hasCriticalMissingData = true
            if (config.rejectIfUnknownFare) {
                rejectionReasons += "Tarifa no detectada"
            } else {
                reviewReasons += "Tarifa no detectada"
            }
        }

        val totalKm = sumIfComplete(input.pickupKm, input.tripKm)
        if (totalKm == null) {
            hasCriticalMissingData = true
            if (config.rejectIfUnknownDistance) {
                rejectionReasons += "Distancia incompleta"
            } else {
                reviewReasons += "Distancia incompleta"
            }
        }

        val totalMinutes = sumIfComplete(input.pickupMinutes, input.tripMinutes)
        if (totalMinutes == null) {
            hasCriticalMissingData = true
            reviewReasons += "Tiempo incompleto"
        }

        if (isOcrAmbiguous) {
            reviewReasons += "OCR ambiguo"
        }

        val arsPerKm = calculateArsPerKm(input.fareAmount, totalKm)
        val arsPerHour = calculateArsPerHour(input.fareAmount, totalMinutes)
        val estimatedCost = calculateEstimatedCost(totalKm, totalMinutes, config)
        val estimatedNetProfit = input.fareAmount?.let { fare ->
            estimatedCost?.let { cost -> fare - cost }
        }

        evaluateArsPerKm(
            value = arsPerKm,
            input = input,
            arsPerHour = arsPerHour,
            estimatedNetProfit = estimatedNetProfit,
            config = config,
            rejectionReasons = rejectionReasons,
            reviewReasons = reviewReasons
        )

        evaluateMinimumMetric(
            value = arsPerHour,
            minimum = config.minArsPerHour,
            tolerancePercent = config.reviewTolerancePercent,
            rejectionReason = "$/hora por debajo del minimo",
            reviewReason = "$/hora dentro de tolerancia",
            rejectionReasons = rejectionReasons,
            reviewReasons = reviewReasons
        )

        if (estimatedNetProfit != null && estimatedNetProfit < config.minNetProfit) {
            rejectionReasons += "Ganancia neta por debajo del minimo"
        }

        if (zoneMatch != null) {
            when (zoneMatch.policy) {
                AvoidZonePolicy.REVIEW -> reviewReasons += "Zona de revision: ${zoneMatch.rule.name}"
                AvoidZonePolicy.REJECT -> {
                    if (config.rejectIfAvoidZoneDetected && !hasCriticalMissingData && !isOcrAmbiguous) {
                        rejectionReasons += "Zona bloqueada: ${zoneMatch.rule.name}"
                    } else {
                        reviewReasons += "Zona bloqueada detectada: ${zoneMatch.rule.name}"
                    }
                }
            }
        }

        val decision = when {
            rejectionReasons.isNotEmpty() -> DriverDecision.REJECT
            reviewReasons.isNotEmpty() -> DriverDecision.REVIEW
            else -> DriverDecision.ACCEPT
        }

        return TripDecisionResult(
            decision = decision,
            fareAmount = input.fareAmount,
            arsPerKm = arsPerKm,
            arsPerHour = arsPerHour,
            estimatedCost = estimatedCost,
            estimatedNetProfit = estimatedNetProfit,
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            rejectionReasons = rejectionReasons,
            reviewReasons = reviewReasons
        )
    }

    private fun sumIfComplete(first: Double?, second: Double?): Double? {
        return if (first != null && second != null) first + second else null
    }

    private fun calculateArsPerKm(fareAmount: Double?, totalKm: Double?): Double? {
        return fareAmount.divideByPositive(totalKm)
    }

    private fun calculateArsPerHour(fareAmount: Double?, totalMinutes: Double?): Double? {
        return fareAmount.divideByPositive(totalMinutes?.div(60.0))
    }

    private fun calculateEstimatedCost(
        totalKm: Double?,
        totalMinutes: Double?,
        config: DriverConfig
    ): Double? {
        return if (totalKm != null && totalMinutes != null) {
            (totalKm * config.costPerKm) + (totalMinutes * config.costPerMinute)
        } else {
            null
        }
    }

    private fun Double?.divideByPositive(denominator: Double?): Double? {
        return if (this != null && denominator != null && denominator > 0.0) {
            this / denominator
        } else {
            null
        }
    }

    private fun evaluateMinimumMetric(
        value: Double?,
        minimum: Double,
        tolerancePercent: Double,
        rejectionReason: String,
        reviewReason: String,
        rejectionReasons: MutableList<String>,
        reviewReasons: MutableList<String>
    ) {
        if (value == null) return

        val toleratedMinimum = minimum * (1.0 - (tolerancePercent / 100.0))
        when {
            value < toleratedMinimum -> rejectionReasons += rejectionReason
            value < minimum -> reviewReasons += reviewReason
        }
    }

    private fun evaluateArsPerKm(
        value: Double?,
        input: TripOfferInput,
        arsPerHour: Double?,
        estimatedNetProfit: Double?,
        config: DriverConfig,
        rejectionReasons: MutableList<String>,
        reviewReasons: MutableList<String>
    ) {
        if (value == null) return

        val toleratedMinimum = config.minArsPerKm * (1.0 - (config.reviewTolerancePercent / 100.0))
        when {
            value < toleratedMinimum && shouldReviewLowArsPerKm(
                arsPerKm = value,
                input = input,
                arsPerHour = arsPerHour,
                estimatedNetProfit = estimatedNetProfit,
                config = config
            ) -> reviewReasons += "$/km bajo, pero $/h alto: revisar destino"
            value < toleratedMinimum -> rejectionReasons += "$/km muy bajo para el viaje"
            value < config.minArsPerKm -> reviewReasons += "$/km dentro de tolerancia"
        }
    }

    private fun shouldReviewLowArsPerKm(
        arsPerKm: Double,
        input: TripOfferInput,
        arsPerHour: Double?,
        estimatedNetProfit: Double?,
        config: DriverConfig
    ): Boolean {
        if (arsPerHour == null || estimatedNetProfit == null) return false
        if (hasExcessivePickupShare(input)) return false

        val rescueMinimumArsPerKm = config.minArsPerKm * LOW_ARS_PER_KM_REVIEW_RESCUE_RATIO
        val highArsPerHourMinimum = config.minArsPerHour * HIGH_ARS_PER_HOUR_REVIEW_RESCUE_RATIO

        return arsPerKm >= rescueMinimumArsPerKm &&
            arsPerHour >= highArsPerHourMinimum &&
            estimatedNetProfit >= config.minNetProfit
    }

    private fun hasExcessivePickupShare(input: TripOfferInput): Boolean {
        val pickupKm = input.pickupKm ?: return false
        val tripKm = input.tripKm ?: return false

        return pickupKm > tripKm
    }

    private companion object {
        const val LOW_ARS_PER_KM_REVIEW_RESCUE_RATIO = 0.70
        const val HIGH_ARS_PER_HOUR_REVIEW_RESCUE_RATIO = 1.50
    }
}
