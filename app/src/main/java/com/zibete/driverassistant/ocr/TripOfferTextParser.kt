package com.zibete.driverassistant.ocr

import com.zibete.driverassistant.debug.DriverAssistantDebugLogger

class TripOfferTextParser(
    private val textSanitizer: RecognizedTextSanitizer = RecognizedTextSanitizer()
) {
    fun parse(rawText: String): TripOfferCandidate? {
        DriverAssistantDebugLogger.log("parser rawText", rawText)
        if (rawText.isBlank()) {
            DriverAssistantDebugLogger.log("parser result", "blank raw text")
            return null
        }

        val sanitizedText = textSanitizer.sanitize(rawText)
        DriverAssistantDebugLogger.log("parser sanitizedText", sanitizedText)
        if (sanitizedText.isBlank()) {
            DriverAssistantDebugLogger.log("parser result", "blank sanitized text")
            return null
        }

        val platform = parsePlatform(sanitizedText)
        val structuredTripFields = parseStructuredTripFields(sanitizedText)
        val hasStructuredTripFields = structuredTripFields.hasAnyField()
        val genericDistances = if (hasStructuredTripFields) {
            emptyList()
        } else {
            distanceRegex.findAll(sanitizedText)
                .mapNotNull { it.groupValues[1].toDecimalOrNull() }
                .toList()
        }
        val genericTimes = if (hasStructuredTripFields) {
            emptyList()
        } else {
            parseTimes(sanitizedText)
        }
        val fare = if (hasStructuredTripFields) {
            parseStructuredFare(sanitizedText)
        } else {
            parseFare(sanitizedText)
        }
        val pickupKm = if (hasStructuredTripFields) {
            structuredTripFields.pickupKm
        } else {
            genericDistances.getOrNull(1)?.let { genericDistances.firstOrNull() }
        }
        val tripKm = if (hasStructuredTripFields) {
            structuredTripFields.tripKm
        } else {
            genericDistances.lastOrNull()
        }
        val pickupMinutes = if (hasStructuredTripFields) {
            structuredTripFields.pickupMinutes
        } else {
            genericTimes.getOrNull(1)?.let { genericTimes.firstOrNull() }
        }
        val tripMinutes = if (hasStructuredTripFields) {
            structuredTripFields.tripMinutes
        } else {
            genericTimes.lastOrNull()
        }

        DriverAssistantDebugLogger.log(
            "parser extracted fields",
            "platform=$platform, hasStructuredTripFields=$hasStructuredTripFields, " +
                "structuredTripFields=$structuredTripFields, genericDistances=$genericDistances, " +
                "genericTimes=$genericTimes, fare=$fare, pickupKm=$pickupKm, tripKm=$tripKm, " +
                "pickupMinutes=$pickupMinutes, tripMinutes=$tripMinutes"
        )

        if (fare == null && pickupKm == null && tripKm == null && pickupMinutes == null && tripMinutes == null && platform == null) {
            DriverAssistantDebugLogger.log("parser result", "no trip fields detected")
            return null
        }

        val parsedFields = buildSet {
            if (fare != null) add(ParsedTripField.FARE)
            if (pickupKm != null || tripKm != null) add(ParsedTripField.DISTANCE)
            if (pickupMinutes != null || tripMinutes != null) add(ParsedTripField.TIME)
            if (platform != null) add(ParsedTripField.PLATFORM)
        }

        val candidate = TripOfferCandidate(
            fareAmount = fare,
            pickupKm = pickupKm,
            tripKm = tripKm,
            pickupMinutes = pickupMinutes,
            tripMinutes = tripMinutes,
            platform = platform,
            rawText = sanitizedText,
            confidence = parsedFields.size / ParsedTripField.entries.size.toDouble()
        )
        DriverAssistantDebugLogger.log("parser candidate", candidate)
        return candidate
    }

    private fun parseFare(rawText: String): Double? {
        val baseFare = parseBaseFare(rawText)
        return baseFare?.plus(parseAdditionalFares(rawText))
    }

    private fun parseBaseFare(rawText: String): Double? {
        return parseExplicitBaseFare(rawText)
            ?: parseStandaloneLargeBaseFare(rawText)
    }

    private fun parseStructuredFare(rawText: String): Double? {
        val lines = rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val firstStructuredLineIndex = lines.indexOfFirst { line ->
            structuredFieldStartRegex.containsMatchIn(line) ||
            structuredPickupRegexes.any { regex -> regex.containsMatchIn(line) } ||
                structuredTripRegexes.any { regex -> regex.containsMatchIn(line) }
        }
        if (firstStructuredLineIndex <= 0) {
            return parseFare(rawText)
        }

        val linesBeforeStructuredFields = lines.take(firstStructuredLineIndex).asReversed()
        val baseFare = linesBeforeStructuredFields
            .filterNot { line -> line.hasAdditionalFareContext() }
            .firstNotNullOfOrNull { line -> parseExplicitFare(line) }
            ?: linesBeforeStructuredFields
                .filterNot { line -> line.hasAdditionalFareContext() }
                .filterNot { line -> line.containsMetricOrNoise() }
                .firstNotNullOfOrNull { line ->
                    standaloneLargeFareRegex.find(line)?.groupValues?.get(1)?.toMoneyOrNull()
                        ?.takeIf { amount -> amount >= MIN_STANDALONE_FARE }
                }

        val additionalFares = linesBeforeStructuredFields
            .asReversed()
            .joinToString("\n")
            .let { parseAdditionalFares(it) }

        return baseFare?.plus(additionalFares)
    }

    private fun parseExplicitBaseFare(rawText: String): Double? {
        return rawText.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() }
            .filterNot { line -> line.hasAdditionalFareContext() }
            .firstNotNullOfOrNull { line -> parseExplicitFare(line) }
    }

    private fun parseExplicitFare(rawText: String): Double? {
        return trailingArsFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
            ?: currencyFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
            ?: pesosFareRegex.find(rawText)?.groupValues?.get(1)?.toMoneyOrNull()
    }

    private fun parseStandaloneLargeBaseFare(rawText: String): Double? {
        return rawText.lineSequence()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() }
            .filterNot { line -> line.hasAdditionalFareContext() }
            .filterNot { line -> line.containsMetricOrNoise() }
            .flatMap { line -> standaloneLargeFareRegex.findAll(line) }
            .mapNotNull { match -> match.groupValues[1].toMoneyOrNull() }
            .firstOrNull { amount -> amount >= MIN_STANDALONE_FARE }
    }

    private fun parseAdditionalFares(rawText: String): Double {
        return rawText.lineSequence()
            .map { it.trim() }
            .filter { line -> line.hasAdditionalFareContext() }
            .flatMap { line -> additionalFareRegex.findAll(line) }
            .mapNotNull { match -> match.groupValues[1].toMoneyOrNull() }
            .sum()
    }

    private fun parseStructuredTripFields(rawText: String): StructuredTripFields {
        val pickupMatch = parseFirstTimeDistanceMatch(rawText, structuredPickupRegexes)
        val tripMatch = parseFirstTimeDistanceMatch(rawText, structuredTripRegexes)

        return StructuredTripFields(
            pickupMinutes = pickupMatch?.minutes,
            pickupKm = pickupMatch?.km,
            tripMinutes = tripMatch?.minutes,
            tripKm = tripMatch?.km
        )
    }

    private fun parseFirstTimeDistanceMatch(
        rawText: String,
        regexes: List<Regex>
    ): TimeDistanceMatch? {
        return regexes.firstNotNullOfOrNull { regex ->
            regex.find(rawText)?.let { match ->
                val minutes = match.groupValues.getOrNull(1)?.toOcrMetricDecimalOrNull()
                val km = match.groupValues.getOrNull(2)?.toOcrMetricDecimalOrNull()
                if (minutes != null && km != null) {
                    TimeDistanceMatch(minutes = minutes, km = km)
                } else {
                    null
                }
            }
        }
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

    private fun String.toOcrMetricDecimalOrNull(): Double? {
        return replace('l', '1')
            .replace('I', '1')
            .replace('|', '1')
            .toDecimalOrNull()
    }

    private fun String.containsMetricOrNoise(): Boolean {
        return metricOrNoiseRegex.containsMatchIn(this)
    }

    private fun String.hasAdditionalFareContext(): Boolean {
        return normalizeAdditionalFareContext().contains("adicional")
    }

    private fun String.normalizeAdditionalFareContext(): String {
        return lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace('1', 'l')
            .replace('I', 'l')
            .replace('|', 'l')
            .replace(Regex("\\s+"), " ")
            .trim()
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

    private data class TimeDistanceMatch(
        val minutes: Double,
        val km: Double
    )

    private companion object {
        private const val MIN_STANDALONE_FARE = 1_000.0
        private const val TIME_DISTANCE_PATTERN =
            """([0-9lI|]+(?:[.,][0-9lI|]+)?)\s*(?:m\s*(?:in|n|inutos)?|rnin)\s*(?:\(\s*)?([0-9lI|]+(?:[.,][0-9lI|]+)?)\s*k\s*(?:m|rn|in)(?:\s*\))?"""

        val trailingArsFareRegex = Regex(
            pattern = """(?i)\b(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?)\s*ARS\b"""
        )
        val currencyFareRegex = Regex(
            pattern = """(?i)(?:ARS\s*|\$\s*)(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?)"""
        )
        val pesosFareRegex = Regex(
            pattern = """(?i)\b(\d{1,3}(?:[.,]\d{3})+|\d+)\s*pesos\b"""
        )
        val additionalFareRegex = Regex(
            pattern = """(?i)(?:\+\s*)?(\d{1,3}(?:[.,]\d{3})+|\d+(?:[.,]\d{1,2})?)\s*ARS\b"""
        )
        val standaloneLargeFareRegex = Regex(
            pattern = """\b(\d{4,6}|\d{1,3}(?:[.,]\d{3})+)\b"""
        )
        val metricOrNoiseRegex = Regex(
            pattern = """(?i)(?:\b(?:min|km|ars|pesos|dni|viaje|distancia)\b|\$/|/\s*(?:h|km)|%|\d+\s*:\s*\d+|\d+[.,]\d+\s*\()"""
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
        val structuredFieldStartRegex = Regex(
            pattern = """(?i)^(?:A\s*[0-9lI|]+|Viaje\b)"""
        )
        val structuredPickupRegexes = listOf(
            Regex(
                pattern = """(?i)\bA\s*$TIME_DISTANCE_PATTERN(?:\s*de\s+distancia)?\b"""
            ),
            Regex(
                pattern = """(?i)(?:^|[^\p{Alnum}])$TIME_DISTANCE_PATTERN\s*de\s+distancia\b"""
            )
        )
        val structuredTripRegexes = listOf(
            Regex(
                pattern = """(?i)\bViaje(?:\s*de)?\s*$TIME_DISTANCE_PATTERN\b"""
            ),
            Regex(
                pattern = """(?is)\bViaje(?:\s*de)?\b.{0,40}?\b$TIME_DISTANCE_PATTERN\b"""
            )
        )
    }
}
