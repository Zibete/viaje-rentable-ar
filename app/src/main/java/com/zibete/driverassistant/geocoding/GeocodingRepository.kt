package com.zibete.driverassistant.geocoding

interface GeocodingRepository {
    suspend fun geocode(
        query: String?,
        bias: GeocodingBias = GeocodingBias.ARGENTINA
    ): GeocodingResult
}
