package com.zibete.driverassistant.geocoding

import com.zibete.driverassistant.home.HomeDirectionCalculator
import com.zibete.driverassistant.home.HomeDirectionStatus
import com.zibete.driverassistant.home.HomeLocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CachedGeocodingRepositoryTest {
    @Test
    fun returnsSuccessfulGeocodingResult() = runBlocking {
        val delegate = FakeGeocodingRepository(success())
        val repository = CachedGeocodingRepository(delegate)

        val result = repository.geocode("Av. Siempre Viva 742")

        assertEquals(GeocodingStatus.SUCCESS, result.status)
        assertEquals(-34.458, result.latitude ?: 0.0, 0.001)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun emptyQueryDoesNotCallDelegate() = runBlocking {
        val delegate = FakeGeocodingRepository(success())
        val repository = CachedGeocodingRepository(delegate)

        val result = repository.geocode("  ")

        assertEquals(GeocodingStatus.EMPTY_QUERY, result.status)
        assertEquals(0, delegate.callCount)
    }

    @Test
    fun cacheAvoidsRepeatedCallsForSameNormalizedText() = runBlocking {
        val delegate = FakeGeocodingRepository(success())
        val repository = CachedGeocodingRepository(delegate)

        repository.geocode(" Av. Siempre Viva 742 ")
        repository.geocode("av. siempre viva 742")

        assertEquals(1, delegate.callCount)
    }

    @Test
    fun cachedFailureAvoidsLoop() = runBlocking {
        val delegate = FakeGeocodingRepository(GeocodingResult.notFound())
        val repository = CachedGeocodingRepository(delegate)

        repository.geocode("Destino inexistente")
        repository.geocode("Destino inexistente")

        assertEquals(1, delegate.callCount)
    }

    @Test
    fun failureCacheExpires() = runBlocking {
        var now = 1_000L
        val delegate = FakeGeocodingRepository(
            GeocodingResult.notFound(),
            success(latitude = -34.459, longitude = -58.915)
        )
        val repository = CachedGeocodingRepository(
            delegate = delegate,
            clockMillis = { now },
            successTtlMillis = 10_000L,
            failureTtlMillis = 100L
        )

        repository.geocode("Destino temporal")
        now += 101L
        val result = repository.geocode("Destino temporal")

        assertEquals(2, delegate.callCount)
        assertEquals(GeocodingStatus.SUCCESS, result.status)
    }

    @Test
    fun lowConfidenceDestinationProducesUnknownHomeScore() {
        val result = success(confidence = 0.4)
        val destination = result.toDestinationLocationOrNull("Destino ambiguo")
        val homeResult = HomeDirectionCalculator().calculate(
            homeLocation = HomeLocation(
                latitude = -34.458,
                longitude = -58.914,
                address = "Casa",
                label = null,
                resolvedAtMillis = null,
                provider = null
            ),
            currentLatitude = -34.500,
            currentLongitude = -58.950,
            destinationText = "Destino ambiguo",
            destinationLocation = destination
        )

        assertEquals(HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION, homeResult.status)
        assertEquals("Casa ?", homeResult.shortLabel)
    }

    private fun success(
        latitude: Double = -34.458,
        longitude: Double = -58.914,
        confidence: Double = 0.9
    ): GeocodingResult {
        return GeocodingResult(
            latitude = latitude,
            longitude = longitude,
            formattedAddress = "Av. Siempre Viva 742, Pilar",
            confidence = confidence,
            status = GeocodingStatus.SUCCESS
        )
    }

    private class FakeGeocodingRepository(
        vararg results: GeocodingResult
    ) : GeocodingRepository {
        private val queuedResults = ArrayDeque(results.toList())
        var callCount = 0
            private set

        override suspend fun geocode(query: String?, bias: GeocodingBias): GeocodingResult {
            callCount += 1
            return queuedResults.removeFirstOrNull() ?: GeocodingResult.notFound()
        }
    }
}
