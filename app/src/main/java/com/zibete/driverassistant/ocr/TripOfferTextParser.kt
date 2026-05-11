package com.zibete.driverassistant.ocr

class TripOfferTextParser {
    fun parse(rawText: String): TripOfferCandidate? {
        if (rawText.isBlank()) return null

        val fare = parseFare(rawText)
        val distances = distanceRegex.findAll(rawText)
            .mapNotNull { it.groupValues[1].toDecimalOrNull() }
            .toList()
        val times = parseTimes(rawText)
        val platform = parsePlatform(rawText)

        if (fare == null && distances.isEmpty() && times.isEmpty() && platform == null) {
            return null
        }

        val parsedFields = buildSet {
            if (fare != null) add(ParsedTripField.FARE)
            if (distances.isNotEmpty()) add(ParsedTripField.DISTANCE)
            if (times.isNotEmpty()) add(ParsedTripField.TIME)
            if (platform != null) add(ParsedTripField.PLATFORM)
        }

        return TripOfferCandidate(
            fareAmount = fare,
            pickupKm = distances.getOrNull(1)?.let { distances.firstOrNull() },
            tripKm = distances.lastOrNull(),
            pickupMinutes = times.getOrNull(1)?.let { times.firstOrNull() },
            tripMinutes = times.lastOrNull(),
            platform = platform,
            rawText = rawText,
            confidence = parsedFields.size / ParsedTripField.entries.size.toDouble()
        )
    }

    private fun parseFare(rawText: String): Double? {
        return currencyFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
            ?: pesosFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
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

    private companion object {
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
    }
}

