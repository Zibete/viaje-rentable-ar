package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import kotlin.math.roundToInt

data class OverlayCardState(
    val decision: DriverDecision,
    val fareText: String,
    val arsPerHourText: String,
    val arsPerKmText: String,
    val totalTimeText: String,
    val totalKmText: String,
    val shortReason: String? = null
) {
    companion object {
        fun fromDecisionResult(result: TripDecisionResult): OverlayCardState {
            return OverlayCardState(
                decision = result.decision,
                fareText = result.fareAmount.toMoneyText(),
                arsPerHourText = result.arsPerHour.toMoneyText(),
                arsPerKmText = result.arsPerKm.toMoneyText(),
                totalTimeText = "${result.totalMinutes.toDisplayNumber()} min",
                totalKmText = "${result.totalKm.toDisplayNumber()} km",
                shortReason = result.rejectionReasons.firstOrNull()
                    ?: result.reviewReasons.firstOrNull()
                    ?: if (!result.hasCompleteOverlayData()) "Datos incompletos" else null
            )
        }

        fun simulatedAccept(): OverlayCardState {
            return OverlayCardState(
                decision = DriverDecision.ACCEPT,
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
    }
}
