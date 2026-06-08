package com.zibete.driverassistant.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverLocationCacheTest {
    @Test
    fun updateStoresValidLocationAsFresh() {
        var now = 1_000L
        val cache = DriverLocationCache(ttlMillis = 10_000L) { now }
        val location = location()

        assertTrue(cache.update(location))

        val cached = cache.getFreshLocation()
        assertEquals(location, cached?.location)
        assertEquals(0L, cached?.ageMillis)
        assertTrue(cache.snapshot().isFresh)

        now += 2_500L
        assertEquals(2_500L, cache.getFreshLocation()?.ageMillis)
    }

    @Test
    fun expiredLocationIsNotFresh() {
        var now = 1_000L
        val cache = DriverLocationCache(ttlMillis = 5_000L) { now }

        cache.update(location())
        now += 5_001L

        assertNull(cache.getFreshLocation())
        val snapshot = cache.snapshot()
        assertTrue(snapshot.hasLocation)
        assertFalse(snapshot.isFresh)
        assertEquals(5_001L, snapshot.ageMillis)
    }

    @Test
    fun invalidLocationIsNotStored() {
        val cache = DriverLocationCache(ttlMillis = 5_000L)

        assertFalse(
            cache.update(
                DriverLocation(
                    latitude = 120.0,
                    longitude = -58.4,
                    accuracyMeters = null,
                    capturedAtMillis = null
                )
            )
        )
        assertNull(cache.getFreshLocation())
    }

    private fun location(): DriverLocation {
        return DriverLocation(
            latitude = -34.5,
            longitude = -58.4,
            accuracyMeters = 20f,
            capturedAtMillis = 1_700_000_000_000L
        )
    }
}
