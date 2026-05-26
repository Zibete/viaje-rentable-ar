package com.zibete.driverassistant.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class AndroidGeocodingRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val textNormalizer: GeocodingTextNormalizer = GeocodingTextNormalizer()
) : GeocodingRepository {
    private val appContext = context.applicationContext

    override suspend fun geocode(
        query: String?,
        bias: GeocodingBias
    ): GeocodingResult {
        val normalizedQuery = textNormalizer.normalizeQuery(query, bias)
            ?: return GeocodingResult.emptyQuery()

        if (!Geocoder.isPresent()) {
            return GeocodingResult.notAvailable()
        }

        return withContext(dispatcher) {
            runCatching {
                val geocoder = Geocoder(
                    appContext,
                    Locale.Builder()
                        .setLanguage("es")
                        .setRegion("AR")
                        .build()
                )
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(
                    normalizedQuery,
                    MAX_RESULTS,
                    bias.lowerLeftLatitude,
                    bias.lowerLeftLongitude,
                    bias.upperRightLatitude,
                    bias.upperRightLongitude
                ).orEmpty()

                addresses.toGeocodingResult(normalizedQuery, bias)
            }.getOrElse { error ->
                when (error) {
                    is IOException -> GeocodingResult.error("geocoder unavailable")
                    else -> GeocodingResult.error(error.message)
                }
            }
        }
    }

    private fun List<Address>.toGeocodingResult(
        normalizedQuery: String,
        bias: GeocodingBias
    ): GeocodingResult {
        val validAddresses = filter { address ->
            address.hasLatitude() &&
                address.hasLongitude() &&
                address.latitude in -90.0..90.0 &&
                address.longitude in -180.0..180.0
        }
        val selected = validAddresses.firstOrNull() ?: return GeocodingResult.notFound()

        return GeocodingResult(
            latitude = selected.latitude,
            longitude = selected.longitude,
            formattedAddress = selected.toSingleLineAddress() ?: normalizedQuery,
            confidence = selected.estimateConfidence(
                originalQuery = normalizedQuery,
                resultCount = validAddresses.size,
                bias = bias
            ),
            status = GeocodingStatus.SUCCESS
        )
    }

    private fun Address.toSingleLineAddress(): String? {
        val lines = (0..maxAddressLineIndex)
            .mapNotNull { index -> getAddressLine(index)?.trim() }
            .filter { it.isNotBlank() }

        return lines.firstOrNull()
            ?: listOfNotNull(thoroughfare, subThoroughfare, locality, adminArea, countryName)
                .joinToString(", ")
                .takeIf { it.isNotBlank() }
    }

    private fun Address.estimateConfidence(
        originalQuery: String,
        resultCount: Int,
        bias: GeocodingBias
    ): Double {
        var confidence = if (resultCount <= 1) 0.90 else 0.70
        if (!isInsideBias(bias)) confidence = minOf(confidence, 0.30)
        if (!hasSpecificAddressSignal()) confidence = minOf(confidence, 0.50)
        if (originalQuery.isAmbiguousGeocodingText()) confidence = minOf(confidence, 0.45)
        return confidence.coerceIn(0.0, 1.0)
    }

    private fun Address.isInsideBias(bias: GeocodingBias): Boolean {
        return latitude in bias.lowerLeftLatitude..bias.upperRightLatitude &&
            longitude in bias.lowerLeftLongitude..bias.upperRightLongitude
    }

    private fun Address.hasSpecificAddressSignal(): Boolean {
        return !featureName.isNullOrBlank() ||
            !thoroughfare.isNullOrBlank() ||
            !locality.isNullOrBlank() ||
            !subAdminArea.isNullOrBlank() ||
            !adminArea.isNullOrBlank()
    }

    private fun String.isAmbiguousGeocodingText(): Boolean {
        val words = split(Regex("\\s+")).filter { it.length >= 3 }
        return length < 6 || words.size < 2
    }

    private companion object {
        const val MAX_RESULTS = 3
    }
}
