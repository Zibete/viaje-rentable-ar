package com.zibete.driverassistant.geocoding

import com.zibete.driverassistant.home.DestinationLocation

fun GeocodingResult.toDestinationLocationOrNull(sourceText: String): DestinationLocation? {
    if (!isResolved) return null

    return DestinationLocation(
        latitude = requireNotNull(latitude),
        longitude = requireNotNull(longitude),
        sourceText = sourceText,
        confidence = confidence
    )
}
