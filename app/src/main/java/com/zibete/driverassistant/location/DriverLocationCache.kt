package com.zibete.driverassistant.location

class DriverLocationCache(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private var cachedLocation: DriverLocation? = null
    private var cachedAtMillis: Long? = null

    @Synchronized
    fun update(location: DriverLocation): Boolean {
        if (!location.hasValidCoordinates()) return false

        cachedLocation = location
        cachedAtMillis = nowMillis()
        return true
    }

    @Synchronized
    fun getFreshLocation(): CachedDriverLocation? {
        val location = cachedLocation ?: return null
        val cachedAt = cachedAtMillis ?: return null
        val ageMillis = nowMillis() - cachedAt
        return if (ageMillis in 0..ttlMillis) {
            CachedDriverLocation(
                location = location,
                cachedAtMillis = cachedAt,
                ageMillis = ageMillis
            )
        } else {
            null
        }
    }

    @Synchronized
    fun snapshot(): DriverLocationCacheSnapshot {
        val location = cachedLocation
        val cachedAt = cachedAtMillis
        val ageMillis = cachedAt?.let { nowMillis() - it }
        val isFresh = location != null && ageMillis != null && ageMillis in 0..ttlMillis
        return DriverLocationCacheSnapshot(
            hasLocation = location != null,
            isFresh = isFresh,
            ageMillis = ageMillis
        )
    }

    @Synchronized
    fun clear() {
        cachedLocation = null
        cachedAtMillis = null
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 90_000L
    }
}

data class CachedDriverLocation(
    val location: DriverLocation,
    val cachedAtMillis: Long,
    val ageMillis: Long
)

data class DriverLocationCacheSnapshot(
    val hasLocation: Boolean,
    val isFresh: Boolean,
    val ageMillis: Long?
)
