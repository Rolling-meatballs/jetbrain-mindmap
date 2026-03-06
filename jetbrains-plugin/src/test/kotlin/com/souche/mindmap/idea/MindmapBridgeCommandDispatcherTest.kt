package com.souche.mindmap.idea

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindmapBridgeCommandDispatcherTest {
    @Test
    fun `loaded command triggers callback`() {
        var loadedCalled = false
        val dispatcher = dispatcher(
            onLoaded = { loadedCalled = true }
        )

        dispatcher.dispatch("""{"command":"loaded"}""")

        assertTrue(loadedCalled)
    }

    @Test
    fun `save and export commands forward payload`() {
        var savePayload = ""
        var exportPayload = ""
        val logs = mutableListOf<String>()
        val dispatcher = MindmapBridgeCommandDispatcher(
            log = { logs += it },
            notifyError = { error("unexpected error: $it") },
            onLoaded = {},
            onSave = { savePayload = it.optString("exportData") },
            onExportToImage = { exportPayload = it.optString("exportData") }
        )

        dispatcher.dispatch("""{"command":" save ","exportData":"{\"root\":{}}"}""")
        dispatcher.dispatch("""{"command":"exportToImage","exportData":"data:image/png;base64,AAAA"}""")

        assertEquals("""{"root":{}}""", savePayload)
        assertEquals("data:image/png;base64,AAAA", exportPayload)
        assertTrue(logs.any { it.startsWith("save_payload_chars=") })
        assertTrue(logs.any { it.startsWith("export_payload_chars=") })
    }

    @Test
    fun `invalid payload and client error are reported`() {
        val errors = mutableListOf<String>()
        val dispatcher = MindmapBridgeCommandDispatcher(
            log = {},
            notifyError = { errors += it },
            onLoaded = {},
            onSave = {},
            onExportToImage = {}
        )

        dispatcher.dispatch("not-json")
        dispatcher.dispatch("""{"command":"clientError","message":"web failed"}""")
        dispatcher.dispatch("""{"command":"clientError","message":"   "}""")

        assertTrue(errors.contains("Invalid message payload from web UI."))
        assertTrue(errors.contains("web failed"))
        assertTrue(errors.contains("Unknown web UI error."))
    }

    @Test
    fun `handler exception is wrapped with command name`() {
        val errors = mutableListOf<String>()
        val dispatcher = MindmapBridgeCommandDispatcher(
            log = {},
            notifyError = { errors += it },
            onLoaded = {},
            onSave = { throw IllegalStateException("boom") },
            onExportToImage = {}
        )

        dispatcher.dispatch(JSONObject().put("command", "save").toString())

        assertEquals(1, errors.size)
        assertTrue(errors.first().contains("Bridge handler failed for 'save'"))
        assertTrue(errors.first().contains("boom"))
    }

    private fun dispatcher(onLoaded: () -> Unit): MindmapBridgeCommandDispatcher {
        return MindmapBridgeCommandDispatcher(
            log = {},
            notifyError = {},
            onLoaded = onLoaded,
            onSave = {},
            onExportToImage = {}
        )
    }
}
