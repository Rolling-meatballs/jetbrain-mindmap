package com.souche.mindmap.idea

import org.json.JSONObject

internal class MindmapBridgeCommandDispatcher(
    private val log: (String) -> Unit,
    private val notifyError: (String) -> Unit,
    private val onLoaded: () -> Unit,
    private val onSave: (JSONObject) -> Unit,
    private val onExportToImage: (JSONObject) -> Unit
) {
    fun dispatch(payload: String?) {
        if (payload.isNullOrBlank()) {
            log("empty payload")
            return
        }

        val payloadJson = runCatching { JSONObject(payload) }
            .onFailure {
                log("invalid json payload")
                notifyError("Invalid message payload from web UI.")
            }
            .getOrNull() ?: return

        val rawCommand = payloadJson.optString("command")
        val command = rawCommand.trim()
        val details = payloadJson.optString("message")
        if (details.isNotBlank()) {
            log("command=$command raw=$rawCommand message=$details")
        } else {
            log("command=$command raw=$rawCommand")
        }

        runCatching {
            when (command) {
                "loaded" -> onLoaded()
                "save" -> {
                    log("save_payload_chars=${payloadJson.optString("exportData").length}")
                    onSave(payloadJson)
                }

                "exportToImage" -> {
                    log("export_payload_chars=${payloadJson.optString("exportData").length}")
                    onExportToImage(payloadJson)
                }

                "clientError" -> notifyError(payloadJson.optString("message").ifBlank { "Unknown web UI error." })
                "debugProbe" -> Unit
            }
        }.onFailure {
            log("handler_exception=${it::class.simpleName}:${it.message ?: "unknown"}")
            notifyError("Bridge handler failed for '$command': ${it.message ?: "Unknown error"}")
        }
    }
}
