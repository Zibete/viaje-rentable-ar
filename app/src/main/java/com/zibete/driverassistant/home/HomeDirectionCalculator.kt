package com.zibete.driverassistant.home

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class HomeDirectionCalculator(
    private val lowConfidenceThreshold: Double = DEFAULT_LOW_CONFIDENCE_THRESHOLD
) {
    fun calculate(
        homeLocation: HomeLocation?,
        currentLatitude: Double?,
        currentLongitude: Double?,
        destinationText: String?,
        destinationLocation: DestinationLocation?
    ): HomeDirectionResult {
        if (homeLocation == null || !homeLocation.hasValidCoordinates()) {
            return HomeDirectionResult.unavailable(HomeDirectionStatus.MISSING_HOME_LOCATION)
        }
        if (destinationText.isNullOrBlank()) {
            return HomeDirectionResult.unavailable(HomeDirectionStatus.MISSING_DESTINATION_TEXT)
        }
        if (destinationLocation == null || !destinationLocation.hasValidCoordinates()) {
            return HomeDirectionResult.unavailable(HomeDirectionStatus.DESTINATION_NOT_RESOLVED)
        }
        if ((destinationLocation.confidence ?: 1.0) < lowConfidenceThreshold) {
            return HomeDirectionResult.unavailable(HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION)
        }
        if (!currentLatitude.isValidLatitude() || !currentLongitude.isValidLongitude()) {
            return HomeDirectionResult.unavailable(HomeDirectionStatus.MISSING_CURRENT_LOCATION)
        }

        val current = GeoPoint(
            latitude = requireNotNull(currentLatitude),
            longitude = requireNotNull(currentLongitude)
        )
        val home = GeoPoint(
            latitude = homeLocation.latitude,
            longitude = homeLocation.longitude
        )
        val destination = GeoPoint(
            latitude = destinationLocation.latitude,
            longitude = destinationLocation.longitude
        )

        val distanceNowToHomeKm = current.distanceToKm(home)
        val distanceDestinationToHomeKm = destination.distanceToKm(home)
        val distanceCurrentToDestinationKm = current.distanceToKm(destination)
        val deltaToHomeKm = distanceNowToHomeKm - distanceDestinationToHomeKm
        val score = calculateScore(
            current = current,
            home = home,
            destination = destination,
            distanceNowToHomeKm = distanceNowToHomeKm,
            distanceDestinationToHomeKm = distanceDestinationToHomeKm,
            distanceCurrentToDestinationKm = distanceCurrentToDestinationKm,
            deltaToHomeKm = deltaToHomeKm
        )

        return HomeDirectionResult(
            status = HomeDirectionStatus.AVAILABLE,
            score = score,
            distanceNowToHomeKm = distanceNowToHomeKm,
            distanceDestinationToHomeKm = distanceDestinationToHomeKm,
            deltaToHomeKm = deltaToHomeKm,
            shortLabel = "Casa $score/10"
        )
    }

    private fun calculateScore(
        current: GeoPoint,
        home: GeoPoint,
        destination: GeoPoint,
        distanceNowToHomeKm: Double,
        distanceDestinationToHomeKm: Double,
        distanceCurrentToDestinationKm: Double,
        deltaToHomeKm: Double
    ): Int {
        if (distanceDestinationToHomeKm <= HOME_NEAR_KM) {
            return 10
        }

        val nearHomeThresholdKm = maxOf(HOME_NEAR_KM, distanceNowToHomeKm * NEAR_HOME_RATIO)
        val minimumNearHomeScore = if (distanceDestinationToHomeKm <= nearHomeThresholdKm) 9 else null
        val progressScaleKm = maxOf(distanceCurrentToDestinationKm, distanceNowToHomeKm * PROGRESS_HOME_SCALE_RATIO, 1.0)
        val progressRatio = (deltaToHomeKm / progressScaleKm).coerceIn(-1.0, 1.0)
        val progressScore = (5.0 + (progressRatio * 5.0)).coerceIn(0.0, 10.0)
        val directionScore = current.directionScoreTo(home, destination)
        val weightedScore = (PROGRESS_WEIGHT * progressScore) + (DIRECTION_WEIGHT * directionScore)
        val roundedScore = weightedScore.roundToInt().coerceIn(0, 10)

        return minimumNearHomeScore?.let { maxOf(it, roundedScore) } ?: roundedScore
    }

    private fun GeoPoint.directionScoreTo(home: GeoPoint, destination: GeoPoint): Double {
        if (distanceToKm(destination) < MIN_DIRECTION_DISTANCE_KM || distanceToKm(home) < MIN_DIRECTION_DISTANCE_KM) {
            return 5.0
        }

        val angle = abs(angleDifferenceDegrees(bearingTo(home), bearingTo(destination)))
        return ((180.0 - angle) / 180.0 * 10.0).coerceIn(0.0, 10.0)
    }

    private fun angleDifferenceDegrees(first: Double, second: Double): Double {
        return ((second - first + 540.0) % 360.0) - 180.0
    }

    private fun Double?.isValidLatitude(): Boolean {
        return this != null && this in -90.0..90.0
    }

    private fun Double?.isValidLongitude(): Boolean {
        return this != null && this in -180.0..180.0
    }

    private data class GeoPoint(
        val latitude: Double,
        val longitude: Double
    ) {
        fun distanceToKm(other: GeoPoint): Double {
            val lat1 = latitude.toRadians()
            val lat2 = other.latitude.toRadians()
            val deltaLat = (other.latitude - latitude).toRadians()
            val deltaLon = (other.longitude - longitude).toRadians()
            val a = sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2.0) * sin(deltaLon / 2.0)
            val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
            return EARTH_RADIUS_KM * c
        }

        fun bearingTo(other: GeoPoint): Double {
            val lat1 = latitude.toRadians()
            val lat2 = other.latitude.toRadians()
            val deltaLon = (other.longitude - longitude).toRadians()
            val y = sin(deltaLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
            return (atan2(y, x).toDegrees() + 360.0) % 360.0
        }

        private fun Double.toRadians(): Double = this * PI / 180.0

        private fun Double.toDegrees(): Double = this * 180.0 / PI
    }

    private companion object {
        const val DEFAULT_LOW_CONFIDENCE_THRESHOLD = 0.55
        const val EARTH_RADIUS_KM = 6371.0088
        const val PROGRESS_WEIGHT = 0.65
        const val DIRECTION_WEIGHT = 0.35
        const val PROGRESS_HOME_SCALE_RATIO = 0.25
        const val NEAR_HOME_RATIO = 0.15
        const val HOME_NEAR_KM = 1.0
        const val MIN_DIRECTION_DISTANCE_KM = 0.05
    }
}
