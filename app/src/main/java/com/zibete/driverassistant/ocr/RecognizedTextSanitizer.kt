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
            .filterNot { it.isOwnOverlayLine() }
            .joinToString("\n")
    }

    private fun String.isOwnOverlayLine(): Boolean {
        return ownDecisionLabelRegex.matches(this) ||
            ownProfitabilityMetricRegex.containsMatchIn(this) ||
            ownOverlayReasonRegex.containsMatchIn(this)
    }

    private companion object {
        val platformLineRegex = Regex("""(?i)\b(?:uberx?|didi|cabify)\b""")
        val ownDecisionLabelRegex = Regex("""(?i)^(?:aceptar|rechazar|revisar)$""")
        val ownProfitabilityMetricRegex = Regex("""(?i)(?:\$/h|\$/km|\$\s*\d+(?:[.,]\d+)?\s*/\s*(?:h|km))""")
        val ownOverlayReasonRegex = Regex(
            """(?i)(?:por debajo del minimo|dentro de tolerancia|ganancia neta|plataforma no|distancia incompleta|tiempo incompleto)"""
        )
    }
}
