package com.zibete.driverassistant.location

import com.zibete.driverassistant.home.DestinationLocation
import com.zibete.driverassistant.home.HomeDirectionCalculator
import com.zibete.driverassistant.home.HomeDirectionResult
import com.zibete.driverassistant.home.HomeLocation

class DriverLocationHomeDirectionResolver(
    private val driverLocationProvider: DriverLocationProvider,
    private val calculator: HomeDirectionCalculator = HomeDirectionCalculator()
) {
    suspend fun calculate(
        homeLocation: HomeLocation?,
        destinationText: String?,
        destinationLocation: DestinationLocation?
    ): HomeDirectionResult {
        if (homeLocation == null ||
            !homeLocation.hasValidCoordinates() ||
            destinationText.isNullOrBlank() ||
            destinationLocation == null ||
            !destinationLocation.hasValidCoordinates()
        ) {
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
