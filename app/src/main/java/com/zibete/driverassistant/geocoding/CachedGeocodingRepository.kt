package com.zibete.driverassistant.geocoding

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CachedGeocodingRepository(
    private val delegate: GeocodingRepository,
    private val textNormalizer: GeocodingTextNormalizer = GeocodingTextNormalizer(),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val successTtlMillis: Long = DEFAULT_SUCCESS_TTL_MILLIS,
    private val failureTtlMillis: Long = DEFAULT_FAILURE_TTL_MILLIS
) : GeocodingRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CacheEntry>()

    override suspend fun geocode(
        query: String?,
        bias: GeocodingBias
    ): GeocodingResult {
        val key = textNormalizer.normalizeKey(query)
            ?: return GeocodingResult.emptyQuery()
        val now = clockMillis()

        mutex.withLock {
            cache[key]?.takeIf { it.expiresAtMillis > now }?.let { return it.result }
        }

        return mutex.withLock {
            val refreshedNow = clockMillis()
            cache[key]?.takeIf { it.expiresAtMillis > refreshedNow }?.let { return@withLock it.result }

            val result = delegate.geocode(key, bias)
            cache[key] = CacheEntry(
                result = result,
                expiresAtMillis = refreshedNow + result.ttlMillis()
            )
            result
        }
    }

    private fun GeocodingResult.ttlMillis(): Long {
        return if (isResolved) successTtlMillis else failureTtlMillis
    }

    private data class CacheEntry(
        val result: GeocodingResult,
        val expiresAtMillis: Long
    )

    private companion object {
        const val DEFAULT_SUCCESS_TTL_MILLIS = 6 * 60 * 60 * 1000L
        const val DEFAULT_FAILURE_TTL_MILLIS = 2 * 60 * 1000L
    }
}
