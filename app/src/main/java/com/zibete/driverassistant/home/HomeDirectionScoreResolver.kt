package com.zibete.driverassistant.home

import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.toHomeLocationOrNull
import com.zibete.driverassistant.geocoding.GeocodingRepository
import com.zibete.driverassistant.geocoding.toDestinationLocationOrNull
import com.zibete.driverassistant.location.DriverLocationProvider

class HomeDirectionScoreResolver(
    private val geocodingRepository: GeocodingRepository,
    private val driverLocationProvider: DriverLocationProvider,
    private val calculator: HomeDirectionCalculator = HomeDirectionCalculator(),
    private val lowConfidenceThreshold: Double = 0.55
) {
    suspend fun resolve(
        config: DriverConfig,
        input: TripOfferInput
    ): HomeDirectionResult {
        val homeLocation = config.toHomeLocationOrNull()
        val destinationText = input.destinationText

        if (homeLocation == null) {
            return calculator.calculate(
                homeLocation = null,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = null
            )
        }
        if (destinationText.isNullOrBlank()) {
            return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = null,
                destinationLocation = null
            )
        }

        val geocodingResult = geocodingRepository.geocode(destinationText)
        val destinationLocation = geocodingResult.toDestinationLocationOrNull(destinationText)
            ?: return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = null
            )

        if ((destinationLocation.confidence ?: 1.0) < lowConfidenceThreshold) {
            return calculator.calculate(
                homeLocation = homeLocation,
                currentLatitude = null,
                currentLongitude = null,
                destinationText = destinationText,
                destinationLocation = destinationLocation
            )
        }

        val driverLocation = driverLocationProvider.getLastKnownLocation().location
        return calculator.calculate(
            homeLocation = homeLocation,
            currentLatitude = driverLocation?.latitude,
            currentLongitude = driverLocation?.longitude,
            destinationText = destinationText,
            destinationLocation = destinationLocation
        )
    }
}
