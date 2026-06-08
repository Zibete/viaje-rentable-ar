package com.zibete.driverassistant.home

import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.toHomeLocationOrNull
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger
import com.zibete.driverassistant.geocoding.GeocodingRepository
import com.zibete.driverassistant.geocoding.GeocodingTextNormalizer
import com.zibete.driverassistant.geocoding.toDestinationLocationOrNull
import com.zibete.driverassistant.location.DriverLocationProvider

class HomeDirectionScoreResolver(
    private val geocodingRepository: GeocodingRepository,
    private val driverLocationProvider: DriverLocationProvider,
    private val calculator: HomeDirectionCalculator = HomeDirectionCalculator(),
    private val geocodingTextNormalizer: GeocodingTextNormalizer = GeocodingTextNormalizer(),
    private val lowConfidenceThreshold: Double = 0.55
) {
    suspend fun resolve(
        config: DriverConfig,
        input: TripOfferInput
    ): HomeDirectionResult {
        val homeLocation = config.toHomeLocationOrNull()
        val destinationText = input.destinationText
        DriverAssistantDebugLogger.log(
            "home score inputs",
            "hasHomeAddress=${!config.homeAddress.isNullOrBlank()}, " +
                "hasHomeCoordinates=${config.homeLatitude != null && config.homeLongitude != null}, " +
                "destinationNormalized=${geocodingTextNormalizer.normalizeKey(destinationText) ?: "none"}"
        )

        if (homeLocation == null) {
            return calculator.calculate(
                homeLocation = null,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = null
            ).also { result -> logResult(result) }
        }
        if (destinationText.isNullOrBlank()) {
            return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = null,
                destinationLocation = null
            ).also { result -> logResult(result) }
        }

        val geocodingResult = geocodingRepository.geocode(destinationText)
        DriverAssistantDebugLogger.log(
            "home destination geocoding",
            "status=${geocodingResult.status}, hasCoordinates=${geocodingResult.hasValidCoordinates}, " +
                "confidence=${geocodingResult.confidence}"
        )
        val destinationLocation = geocodingResult.toDestinationLocationOrNull(destinationText)
            ?: return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = null
            ).also { result -> logResult(result) }

        if ((destinationLocation.confidence ?: 1.0) < lowConfidenceThreshold) {
            return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = destinationLocation
            ).also { result -> logResult(result) }
        }

        val driverLocationResult = driverLocationProvider.getLastKnownLocation()
        DriverAssistantDebugLogger.log(
            "home driver location",
            "status=${driverLocationResult.status}, hasLocation=${driverLocationResult.location != null}"
        )
        val driverLocation = driverLocationResult.location
        return calculator.calculate(
            homeLocation = homeLocation,
            currentLatitude = driverLocation?.latitude,
            currentLongitude = driverLocation?.longitude,
            destinationText = destinationText,
            destinationLocation = destinationLocation
        ).also { result -> logResult(result) }
    }

    private fun logResult(result: HomeDirectionResult) {
        DriverAssistantDebugLogger.log(
            "home direction result",
            "status=${result.status}, score=${result.score}, label=${result.toDiagnosticShortLabel()}"
        )
    }
}
