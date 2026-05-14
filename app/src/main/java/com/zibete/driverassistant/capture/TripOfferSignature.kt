package com.zibete.driverassistant.capture

import com.zibete.driverassistant.calculator.TripDecisionResult

data class TripOfferSignature(
    val fareAmount: Double?,
    val totalKm: Double?,
    val totalMinutes: Double?,
    val normalizedTextHash: Int
) {
    companion object {
        fun fromDecisionResult(
            result: TripDecisionResult,
            rawText: String?
        ): TripOfferSignature {
            return TripOfferSignature(
                fareAmount = result.fareAmount?.roundForSignature(),
                totalKm = result.totalKm?.roundForSignature(),
                totalMinutes = result.totalMinutes?.roundForSignature(),
                normalizedTextHash = rawText.normalizeForSignature().hashCode()
            )
        }

        private fun Double.roundForSignature(): Double {
            return kotlin.math.round(this * 10.0) / 10.0
        }

        private fun String?.normalizeForSignature(): String {
            return orEmpty()
                .lowercase()
                .replace(Regex("""\s+"""), " ")
                .trim()
        }
    }
}
