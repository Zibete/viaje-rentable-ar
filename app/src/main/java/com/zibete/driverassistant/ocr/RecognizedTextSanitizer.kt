package com.zibete.driverassistant.ocr

class RecognizedTextSanitizer {
    fun sanitize(rawText: String): String {
        val lines = rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return ""

        val platformLineIndex = lines.indexOfFirst { line ->
            platformLineRegex.containsMatchIn(line)
        }
        val candidateLines = if (platformLineIndex > 0) {
            lines.drop(platformLineIndex)
        } else {
            lines
        }

        return candidateLines
            .filterIndexed { index, line -> !line.isOwnOverlayLine(candidateLines, index) }
            .joinToString("\n")
    }

    private fun String.isOwnOverlayLine(lines: List<String>, index: Int): Boolean {
        return ownDecisionLabelRegex.matches(this) ||
            ownProfitabilityMetricRegex.containsMatchIn(this) ||
            ownOverlayReasonRegex.containsMatchIn(this) ||
            (ownOverlayFareRegex.matches(this) && hasAdjacentOwnOverlayLine(lines, index))
    }

    private fun hasAdjacentOwnOverlayLine(lines: List<String>, index: Int): Boolean {
        val previous = lines.getOrNull(index - 1)
        val next = lines.getOrNull(index + 1)
        return listOfNotNull(previous, next).any { line ->
            ownDecisionLabelRegex.matches(line) ||
                ownProfitabilityMetricRegex.containsMatchIn(line) ||
                ownOverlayReasonRegex.containsMatchIn(line)
        }
    }

    private companion object {
        val platformLineRegex = Regex("""(?i)\b(?:uberx?|didi|cabify)\b""")
        val ownDecisionLabelRegex = Regex("""(?i)^(?:aceptar|rechazar|revisar)$""")
        val ownOverlayFareRegex = Regex("""(?i)^\$\s*\d+(?:[.,]\d+)?$""")
        val ownProfitabilityMetricRegex = Regex("""(?i)(?:\$/h|\$/km|\$\s*\d+(?:[.,]\d+)?\s*/\s*(?:h|km))""")
        val ownOverlayReasonRegex = Regex(
            """(?i)(?:por debajo del minimo|dentro de tolerancia|ganancia neta|plataforma no|distancia incompleta|tiempo incompleto)"""
        )
    }
}
