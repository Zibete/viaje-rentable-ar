package com.zibete.driverassistant.geocoding

import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger

class HomeAddressConfigResolver(
    private val geocodingRepository: GeocodingRepository,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val minimumConfidence: Double = 0.55
) {
    suspend fun resolveIfNeeded(config: DriverConfig): HomeAddressConfigResolution {
        val address = config.homeAddress?.trim()
        DriverAssistantDebugLogger.log(
            "home config resolve inputs",
            "hasHomeAddress=${!address.isNullOrBlank()}, " +
                "hasHomeCoordinates=${config.homeLatitude != null && config.homeLongitude != null}"
        )
        if (address.isNullOrBlank()) {
            return HomeAddressConfigResolution(
                config = config.copy(
                    homeAddress = null,
                    homeLatitude = null,
                    homeLongitude = null,
                    homeAddressLabel = null,
                    homeResolvedAtMillis = null,
                    homeGeocodingProvider = null
                ),
                status = HomeAddressConfigResolutionStatus.NO_ADDRESS
            ).also { resolution -> logResolution(resolution) }
        }

        val latitude = config.homeLatitude
        val longitude = config.homeLongitude
        if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0) {
            return HomeAddressConfigResolution(
                config = config,
                status = HomeAddressConfigResolutionStatus.ALREADY_RESOLVED
            ).also { resolution -> logResolution(resolution) }
        }

        val result = geocodingRepository.geocode(address)
        DriverAssistantDebugLogger.log(
            "home config geocoding",
            "status=${result.status}, hasCoordinates=${result.hasValidCoordinates}, confidence=${result.confidence}"
        )
        if (!result.isResolved || (result.confidence ?: 0.0) < minimumConfidence) {
            return HomeAddressConfigResolution(
                config = config.copy(
                    homeLatitude = null,
                    homeLongitude = null,
                    homeAddressLabel = null,
                    homeResolvedAtMillis = null,
                    homeGeocodingProvider = null
                ),
                status = HomeAddressConfigResolutionStatus.NOT_RESOLVED
            ).also { resolution -> logResolution(resolution) }
        }

        return HomeAddressConfigResolution(
            config = config.copy(
                homeAddress = address,
                homeLatitude = result.latitude,
                homeLongitude = result.longitude,
                homeAddressLabel = result.formattedAddress,
                homeResolvedAtMillis = clockMillis(),
                homeGeocodingProvider = "android-geocoder"
            ),
            status = HomeAddressConfigResolutionStatus.RESOLVED
        ).also { resolution -> logResolution(resolution) }
    }

    private fun logResolution(resolution: HomeAddressConfigResolution) {
        DriverAssistantDebugLogger.log(
            "home config resolve result",
            "status=${resolution.status}, hasHomeCoordinates=" +
                "${resolution.config.homeLatitude != null && resolution.config.homeLongitude != null}"
        )
    }
}

data class HomeAddressConfigResolution(
    val config: DriverConfig,
    val status: HomeAddressConfigResolutionStatus
)

enum class HomeAddressConfigResolutionStatus {
    NO_ADDRESS,
    ALREADY_RESOLVED,
    RESOLVED,
    NOT_RESOLVED
}
