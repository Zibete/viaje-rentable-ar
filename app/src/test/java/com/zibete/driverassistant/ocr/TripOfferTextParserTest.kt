package com.zibete.driverassistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TripOfferTextParserTest {
    private val parser = TripOfferTextParser()

    @Test
    fun parsesArsAmountMinutesAndDecimalKm() {
        val result = parser.parse("ARS 5.127 41 min 8.0 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesDollarAmountMinutesAndIntegerKm() {
        val result = parser.parse("${'$'} 5127 41 min 8 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesPesosAmountMinutesAndCommaKm() {
        val result = parser.parse("5.127 pesos 41 minutos 8,0 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun ignoresProfitabilityMetricsWhenParsingTripDistance() {
        val result = parser.parse("Aceptar ${'$'} 5.127 7.503/h 641/km 41 min 8.0 km total")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesMinuteRangeAsAverage() {
        val result = parser.parse("1-2 min ${'$'} 5.127 8 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(1.5, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesRealisticUberArgentinaOffer() {
        val result = parser.parse(
            """
            UberX
            7.124 ARS
            DNI verificado
            4,98 (253)
            A 6 min (2.5 km) de distancia
            Unnamed Road, Pilar
            Viaje de 24 min (12.2 km)
            Florida, Jose C. Paz
            Emparejar
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(7124.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(24.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun parsesRealisticUberArgentinaOfferWithCommaDecimals() {
        val result = parser.parse(
            """
            UberX
            7.124 ARS
            A 6 min (2,5 km) de distancia
            Viaje de 24 min (12,2 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(7124.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(24.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun ignoresOwnOverlayWhenParsingUberOffer() {
        val result = parser.parse(
            """
            RECHAZAR
            ${'$'} 10
            ${'$'} 5/h - ${'$'} 0/km
            131,5 min - 44,1 km
            ${'$'}/km por debajo del minimo

            UberX
            7.124 ARS
            A 6 min (2.5 km) de distancia
            Viaje de 24 min (12.2 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(7124.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(24.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun ignoresExtraNumbersAroundStructuredUberOffer() {
        val result = parser.parse(
            """
            RECHAZAR
            ${'$'} 10
            ${'$'} 5/h - ${'$'} 0/km
            131,5 min - 44,1 km
            4,98 (253)
            14:35
            87%
            UberX
            7.124 ARS
            A 6 min (2.5 km) de distancia
            Viaje de 24 min (12.2 km)
            453.2 km
            56.4 km
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(7124.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(24.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun doesNotUseGenericFallbackWhenStructuredUberOfferIsPartial() {
        val result = parser.parse(
            """
            UberX
            7.124 ARS
            A 6 min (2.5 km) de distancia
            453.2 km
            56.4 km
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(7124.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(null, result?.tripMinutes)
        assertEquals(null, result?.tripKm)
    }

    @Test
    fun doesNotUseGenericFareFallbackWhenStructuredUberOfferIsPresent() {
        val result = parser.parse(
            """
            RECHAZAR
            ${'$'} 10
            UberX
            A 6 min (2.5 km) de distancia
            Viaje de 24 min (12.2 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(null, result?.fareAmount)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(24.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
    }
}
