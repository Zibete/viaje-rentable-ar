package com.zibete.driverassistant.debug

import android.util.Log

internal object DriverAssistantDebugLogger {
    private const val TAG = "DriverAssistantDebug"
    private const val CHUNK_SIZE = 3_500

    fun log(label: String, value: Any?) {
        val message = "$label:\n${value ?: "null"}"
        if (message.length <= CHUNK_SIZE) {
            write(message)
            return
        }

        message.chunked(CHUNK_SIZE).forEachIndexed { index, chunk ->
            write("$label [${index + 1}]:\n$chunk")
        }
    }

    private fun write(message: String) {
        // TODO: remover estos logs de diagnostico antes de preparar una build release.
        runCatching { Log.d(TAG, message) }
            .onFailure { println("$TAG: $message") }
    }
}
