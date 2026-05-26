package com.zibete.driverassistant.home

data class HomeDirectionResult(
    val status: HomeDirectionStatus,
    val score: Int?,
    val distanceNowToHomeKm: Double?,
    val distanceDestinationToHomeKm: Double?,
    val deltaToHomeKm: Double?,
    val shortLabel: String
) {
    companion object {
        fun unavailable(status: HomeDirectionStatus): HomeDirectionResult {
            return HomeDirectionResult(
                status = status,
                score = null,
                distanceNowToHomeKm = null,
                distanceDestinationToHomeKm = null,
                deltaToHomeKm = null,
                shortLabel = "Casa ?"
            )
        }
    }
}
