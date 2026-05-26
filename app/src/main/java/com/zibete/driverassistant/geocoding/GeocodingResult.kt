package com.zibete.driverassistant.geocoding

data class GeocodingResult(
    val latitude: Double?,
    val longitude: Double?,
    val formattedAddress: String?,
    val confidence: Double?,
    val status: GeocodingStatus,
    val errorMessage: String? = null
) {
    val hasValidCoordinates: Boolean
        get() = latitude != null &&
            longitude != null &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0

    val isResolved: Boolean
        get() = status == GeocodingStatus.SUCCESS && hasValidCoordinates

    companion object {
        fun emptyQuery(): GeocodingResult {
            return GeocodingResult(
                latitude = null,
                longitude = null,
                formattedAddress = null,
                confidence = null,
                status = GeocodingStatus.EMPTY_QUERY
            )
        }

        fun notAvailable(): GeocodingResult {
            return GeocodingResult(
                latitude = null,
                longitude = null,
                formattedAddress = null,
                confidence = null,
                status = GeocodingStatus.NOT_AVAILABLE
            )
        }

        fun notFound(): GeocodingResult {
            return GeocodingResult(
                latitude = null,
                longitude = null,
                formattedAddress = null,
                confidence = null,
                status = GeocodingStatus.NOT_FOUND
            )
        }

        fun error(message: String?): GeocodingResult {
            return GeocodingResult(
                latitude = null,
                longitude = null,
                formattedAddress = null,
                confidence = null,
                status = GeocodingStatus.ERROR,
                errorMessage = message
            )
        }
    }
}
