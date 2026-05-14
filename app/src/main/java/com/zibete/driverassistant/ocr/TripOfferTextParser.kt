package com.zibete.driverassistant.ocr

class TripOfferTextParser(
    private val textSanitizer: RecognizedTextSanitizer = RecognizedTextSanitizer()
) {
    fun parse(rawText: String): TripOfferCandidate? {
        if (rawText.isBlank()) return null

        val sanitizedText = textSanitizer.sanitize(rawText)
        if (sanitizedText.isBlank()) return null

        val platform = parsePlatform(sanitizedText)
        val uberStructuredFields = if (platform == "uber") {
            parseUberStructuredFields(sanitizedText)
        } else {
            StructuredTripFields()
        }
        val hasUberStructuredFields = platform == "uber" && uberStructuredFields.hasAnyField()
        val genericDistances = if (hasUberStructuredFields) {
            emptyList()
        } else {
            distanceRegex.findAll(sanitizedText)
                .mapNotNull { it.groupValues[1].toDecimalOrNull() }
                .toList()
        }
        val genericTimes = if (hasUberStructuredFields) {
            emptyList()
        } else {
            parseTimes(sanitizedText)
        }
        val fare = if (hasUberStructuredFields) {
            parseUberStructuredFare(sanitizedText)
        } else {
            parseFare(sanitizedText)
        }
        val pickupKm = if (hasUberStructuredFields) {
            uberStructuredFields.pickupKm
        } else {
            genericDistances.getOrNull(1)?.let { genericDistances.firstOrNull() }
        }
        val tripKm = if (hasUberStructuredFields) {
            uberStructuredFields.tripKm
        } else {
            genericDistances.lastOrNull()
        }
        val pickupMinutes = if (hasUberStructuredFields) {
            uberStructuredFields.pickupMinutes
        } else {
            genericTimes.getOrNull(1)?.let { genericTimes.firstOrNull() }
        }
        val tripMinutes = if (hasUberStructuredFields) {
            uberStructuredFields.tripMinutes
        } else {
            genericTimes.lastOrNull()
        }

        if (fare == null && pickupKm == null && tripKm == null && pickupMinutes == null && tripMinutes == null && platform == null) {
            return null
        }

        val parsedFields = buildSet {
            if (fare != null) add(ParsedTripField.FARE)
            if (pickupKm != null || tripKm != null) add(ParsedTripField.DISTANCE)
            if (pickupMinutes != null || tripMinutes != null) add(ParsedTripField.TIME)
            if (platform != null) add(ParsedTripField.PLATFORM)
        }

        return TripOfferCandidate(
            fareAmount = fare,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMinutes,
            tripMinutes = tripMinutes,
            platform = platform,
            rawText = sanitizedText,
            confidence = parsedFields.size / ParsedTripField.entries.size.toDouble()
        )
    }

    private fun parseFare(rawText: String): Double? {
        return parseUberStructuredFare(rawText)
            ?: currencyFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
            ?: pesosFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
    }

    private fun parseUberStructuredFare(rawText: String): Double? {
        return trailingArsFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
    }

    private fun parseUberStructuredFields(rawText: String): StructuredTripFields {
        val pickupMatch = uberPickupRegex.find(rawText)
        val tripMatch = uberTripRegex.find(rawText)

        return StructuredTripFields(
            pickupMinutes = pickupMatch?.groupValues?.getOrNull(1)?.toDecimalOrNull(),
            pickupKm = pickupMatch?.groupValues?.getOrNull(2)?.toDecimalOrNull(),
            tripMinutes = tripMatch?.groupValues?.getOrNull(1)?.toDecimalOrNull(),
            tripKm = tripMatch?.groupValues?.getOrNull(2)?.toDecimalOrNull()
        )
    }

    private fun parseTimes(rawText: String): List<Double> {
        val rangeMatches = minuteRangeRegex.findAll(rawText).toList()
        val rangeSpans = rangeMatches.map { it.range }
        val rangeValues = rangeMatches.mapNotNull { match ->
            val from = match.groupValues[1].toDoubleOrNull()
            val to = match.groupValues[2].toDoubleOrNull()
            if (from != null && to != null) (from + to) / 2.0 else null
        }

        val singleValues = minuteRegex.findAll(rawText)
            .filterNot { single -> rangeSpans.any { single.range.first >= it.first && single.range.last <= it.last } }
            .mapNotNull { it.groupValues[1].toDecimalOrNull() }
            .toList()

        return rangeValues + singleValues
    }

    private fun parsePlatform(rawText: String): String? {
        val normalized = rawText.lowercase()
        return when {
            "uber" in normalized -> "uber"
            "didi" in normalized -> "didi"
            "cabify" in normalized -> "cabify"
            else -> null
        }
    }

    private fun String.toMoneyOrNull(): Double? {
        val normalized = replace(" ", "")
        val lastSeparatorIndex = maxOf(normalized.lastIndexOf('.'), normalized.lastIndexOf(','))
        val integerLike = when {
            lastSeparatorIndex < 0 -> normalized
            normalized.length - lastSeparatorIndex - 1 == 3 -> {
                normalized.replace(".", "").replace(",", "")
            }
            else -> normalized.replace(",", ".")
        }

        return integerLike.toDoubleOrNull()
    }

    private fun String.toDecimalOrNull(): Double? {
        return replace(",", ".").toDoubleOrNull()
    }

    private data class StructuredTripFields(
        val pickupKm: Double? = null,
        val tripKm: Double? = null,
        val pickupMinutes: Double? = null,
        val tripMinutes: Double? = null
    ) {
        fun hasAnyField(): Boolean {
            return pickupKm != null ||
                tripKm != null ||
                pickupMinutes != null ||
                tripMinutes != null
        }
    }

    private companion object {
        val trailingArsFareRegex = Regex(
            pattern = """(?i)\b(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?)\s*ARS\b"""
        )
        val currencyFareRegex = Regex(
            pattern = """(?i)(?:ARS\s*|\$\s*)(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?)"""
        )
        val pesosFareRegex = Regex(
            pattern = """(?i)\b(\d{1,3}(?:[.,]\d{3})+|\d+)\s*pesos\b"""
        )
        val distanceRegex = Regex(
            pattern = """(?i)\b(\d+(?:[.,]\d+)?)\s*km\b"""
        )
        val minuteRangeRegex = Regex(
            pattern = """(?i)\b(\d+)\s*-\s*(\d+)\s*min(?:utos)?\b"""
        )
        val minuteRegex = Regex(
            pattern = """(?i)\b(\d+(?:[.,]\d+)?)\s*min(?:utos)?\b"""
        )
        val uberPickupRegex = Regex(
            pattern = """(?i)\bA\s+(\d+(?:[.,]\d+)?)\s*min(?:utos)?\s*\(\s*(\d+(?:[.,]\d+)?)\s*km\s*\)\s*de\s+distancia\b"""
        )
        val uberTripRegex = Regex(
            pattern = """(?i)\bViaje\s+de\s+(\d+(?:[.,]\d+)?)\s*min(?:utos)?\s*\(\s*(\d+(?:[.,]\d+)?)\s*km\s*\)"""
        )
    }
}
