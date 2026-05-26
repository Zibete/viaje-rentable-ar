package com.zibete.driverassistant.config

import com.zibete.driverassistant.home.HomeLocation

fun DriverConfig.toHomeLocationOrNull(): HomeLocation? {
    val latitude = homeLatitude
    val longitude = homeLongitude
    val address = homeAddress?.trim()
    if (latitude == null || longitude == null || address.isNullOrBlank()) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null

    return HomeLocation(
        latitude = latitude,
        longitude = longitude,
        address = address,
        label = homeAddressLabel,
        resolvedAtMillis = homeResolvedAtMillis,
        provider = homeGeocodingProvider
    )
}
