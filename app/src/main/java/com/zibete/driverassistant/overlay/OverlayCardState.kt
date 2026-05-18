package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import kotlin.math.roundToInt

data class OverlayCardState(
    val decision: DriverDecision,
    val visualState: OverlayVisualState,
    val titleText: String,
    val fareText: String,
    val arsPerHourText: String,
    val arsPerKmText: String,
    val totalTimeText: String,
    val totalKmText: String,
    val shortReason: String? = null
) {
    companion object {
        fun fromDecisionResult(result: TripDecisionResult): OverlayCardState {
            val hasCompleteData = result.hasCompleteOverlayData()
            return OverlayCardState(
                decision = result.decision,
                visualState = if (hasCompleteData) {
                    OverlayVisualState.DECISION
                } else {
                    OverlayVisualState.INCOMPLETE_DATA
                },
                titleText = if (hasCompleteData) {
                    result.decision.toSpanishLabel()
                } else {
                    "DATOS INCOMPLETOS"
                },
                fareText = result.fareAmount.toMoneyText(),
                arsPerHourText = result.arsPerHour.toMoneyText(),
                arsPerKmText = result.arsPerKm.toMoneyText(),
                totalTimeText = "${result.totalMinutes.toDisplayNumber()} min",
                totalKmText = "${result.totalKm.toDisplayNumber()} km",
                shortReason = result.rejectionReasons.firstOrNull()
                    ?: result.reviewReasons.firstOrNull()
                    ?: result.firstMissingOverlayDataReason()
            )
        }

        fun simulatedAccept(): OverlayCardState {
            return OverlayCardState(
                decision = DriverDecision.ACCEPT,
                visualState = OverlayVisualState.DECISION,
                titleText = DriverDecision.ACCEPT.toSpanishLabel(),
                fareText = "$ 5.127",
                arsPerHourText = "$ 7.503",
                arsPerKmText = "$ 641",
                totalTimeText = "41.0 min",
                totalKmText = "8.0 km"
            )
        }

        private fun Double?.toMoneyText(): String {
            return this?.roundToInt()?.let { "$ $it" } ?: "$ -"
        }

        private fun Double?.toDisplayNumber(): String {
            return this?.let { "%.1f".format(it) } ?: "-"
        }

        private fun TripDecisionResult.firstMissingOverlayDataReason(): String? {
            return when {
                hasCompleteOverlayData() -> null
                fareAmount == null -> "Falta tarifa"
                totalKm == null || arsPerKm == null -> "Falta distancia"
                totalMinutes == null || arsPerHour == null -> "Falta tiempo"
                else -> "Datos incompletos"
            }
        }

        private fun DriverDecision.toSpanishLabel(): String {
            return when (this) {
                DriverDecision.ACCEPT -> "ACEPTAR"
                DriverDecision.REJECT -> "RECHAZAR"
                DriverDecision.REVIEW -> "REVISAR"
            }
        }
    }
}

enum class OverlayVisualState {
    DECISION,
    INCOMPLETE_DATA
}
