package com.zibete.driverassistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun doesNotUseSmallOverlayAmountAsStructuredFare() {
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

    @Test
    fun parsesCurrentRealUberOffer() {
        val result = parser.parse(
            """
            UberX
            5.472 ARS
            DNI verificado
            4,89 (18)
            A 3 min (1.3 km) de distancia
            Cabo 1ro Daniel A. Romero, Pilar
            Viaje de 26 min (11.9 km)
            Dr Francisco J. Muñiz 5802, Jose C. Paz
            Aceptar
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun parsesCurrentUberOfferWithoutParentheses() {
        val result = parser.parse(
            """
            UberX
            5.472 ARS
            A 3 min 1.3 km
            Viaje 26 min 11.9 km
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun parsesCurrentUberOfferWithCommaDecimals() {
        val result = parser.parse(
            """
            UberX
            5.472 ARS
            a 3 min (1,3 km)
            Viaje de 26 min (11,9 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun parsesCurrentUberOfferWithLineBreakBeforeDistance() {
        val result = parser.parse(
            """
            UberX
            5.472 ARS
            A 3 min
            (1.3 km) de distancia
            Viaje de 26 min
            (11.9 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesCurrentUberOfferWithJoinedPickupPrefix() {
        val result = parser.parse(
            """
            2 UberX Exclusivo
            5.472 ARS
            ODNI Verificado ★ 4,89 (18)
            A3 min (1.3 km) de distancia
            Cabo lro Daniel A. Romero, Pilar
            Viaje de 26 min (11.9 km)
            Dr. Francisco J. Muñiz 5802, Jose C. Paz
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }

    @Test
    fun parsesStructuredOfferWithoutPlatform() {
        val result = parser.parse(
            """
            2 Comfort Exclusivo
            8.780 ARS
            A6 min (2.5 km) de distancia
            Viaje de 26 min (12.2 km)
            Aceptar
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(8780.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
        assertEquals(null, result?.platform)
    }

    @Test
    fun sumsAdditionalFareWhenBaseFareExists() {
        val result = parser.parse(
            """
            7.915 ARS
            DNI verificado * 5,00 (104)
            +914,00 ARS adicionales por
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(8829.0, result?.fareAmount ?: 0.0, 0.001)
    }

    @Test
    fun keepsBaseFareWhenNoAdditionalFareExists() {
        val result = parser.parse("7.915 ARS")

        assertNotNull(result)
        assertEquals(7915.0, result?.fareAmount ?: 0.0, 0.001)
    }

    @Test
    fun doesNotUseAdditionalFareAsMainFareWithoutBaseFare() {
        val result = parser.parse("+914,00 ARS adicionales por")

        assertTrue(result == null || result.fareAmount == null)
    }

    @Test
    fun parsesAdditionalFareFormatVariants() {
        listOf(
            "+914,00 ARS adicionales",
            "+ 914,00 ARS adicionales",
            "914,00 ARS adicionales",
            "+914 ARS adicional",
            "+914,00 ARS adicionales por"
        ).forEach { additionalLine ->
            val result = parser.parse(
                """
                7.915 ARS
                $additionalLine
                """.trimIndent()
            )

            assertNotNull(result)
            assertEquals(additionalLine, 8829.0, result?.fareAmount ?: 0.0, 0.001)
        }
    }

    @Test
    fun parsesStructuredOfferWithoutPlatformAndAddsAdditionalFare() {
        val result = parser.parse(
            """
            2 Comfort Exclusivo
            7.915 ARS
            +914,00 ARS adicionales por
            A6 min (2.5 km) de distancia
            Viaje de 26 min (12.2 km)
            Aceptar
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(8829.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesJoinedPickupMinutesWithoutPlatform() {
        val result = parser.parse(
            """
            8.780 ARS
            A6 min (2.5 km) de distancia
            Viaje de 26 min (12.2 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
    }

    @Test
    fun ignoresOldOverlayWhenParsingStructuredOfferWithoutPlatform() {
        val result = parser.parse(
            """
            REVISAR
            ${'$'} 5472
            ${'$'} 531/km - ${'$'} -/h
            - min - 10,3 km
            Tiempo incompleto

            2 Comfort Exclusivo
            8.780 ARS
            A6 min (2.5 km) de distancia
            Viaje de 26 min (12.2 km)
            Aceptar
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(8780.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(6.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(2.5, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(12.2, result?.tripKm ?: 0.0, 0.001)
        assertEquals(null, result?.platform)
    }

    @Test
    fun parsesUberOfferWithJoinedTripPrefixVariants() {
        listOf(
            "Viaje26 min (11.9 km)",
            "Viaje de26 min (11.9 km)",
            "Viajede 26 min (11.9 km)"
        ).forEach { tripLine ->
            assertParsesUberMetrics(tripLine = tripLine)
        }
    }

    @Test
    fun parsesUberOfferWithOcrDistanceNumberVariants() {
        listOf(
            "Viaje de 26 min (1l.9 km)",
            "Viaje de 26 min (1I.9 km)",
            "Viaje de 26 min (1|.9 km)",
            "Viaje de 26 min (11.9 k m)",
            "Viaje de 26 min (11.9 krn)",
            "Viaje de 26 min (11.9 kin)"
        ).forEach { tripLine ->
            assertParsesUberMetrics(tripLine = tripLine)
        }

        assertParsesUberMetrics(pickupLine = "A 3 min (l.3 km) de distancia")
    }

    @Test
    fun parsesUberOfferWithOcrMinuteUnitVariants() {
        listOf(
            "A 3 mn (1.3 km) de distancia",
            "A 3 rnin (1.3 km) de distancia",
            "A 3 m in (1.3 km) de distancia"
        ).forEach { pickupLine ->
            assertParsesUberMetrics(pickupLine = pickupLine)
        }
    }

    @Test
    fun parsesStructuredUberFareVariants() {
        listOf(
            "5.472 ARS",
            "5472 ARS",
            "ARS 5.472",
            "ARS 5472",
            "${'$'} 5.472",
            "${'$'} 5472",
            "5472"
        ).forEach { fareLine ->
            val result = parser.parse(
                """
                UberX
                $fareLine
                A 3 min (1.3 km) de distancia
                Viaje de 26 min (11.9 km)
                Aceptar
                """.trimIndent()
            )

            assertNotNull(result)
            assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        }
    }

    @Test
    fun ignoresOldOverlayWhenParsingCurrentUberOffer() {
        val result = parser.parse(
            """
            REVISAR
            ${'$'} 5472
            ${'$'} 531/km - ${'$'} -/h
            - min - 10,3 km
            Tiempo incompleto

            UberX
            5.472 ARS
            A 3 min (1.3 km) de distancia
            Viaje de 26 min (11.9 km)
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
    }

    private fun assertParsesUberMetrics(
        pickupLine: String = "A3 min (1.3 km) de distancia",
        tripLine: String = "Viaje de 26 min (11.9 km)"
    ) {
        val result = parser.parse(
            """
            UberX
            5.472 ARS
            $pickupLine
            $tripLine
            """.trimIndent()
        )

        assertNotNull(result)
        assertEquals(5472.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(3.0, result?.pickupMinutes ?: 0.0, 0.001)
        assertEquals(1.3, result?.pickupKm ?: 0.0, 0.001)
        assertEquals(26.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(11.9, result?.tripKm ?: 0.0, 0.001)
        assertEquals("uber", result?.platform)
    }
}
