package com.souche.mindmap.idea

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindmapBridgeRoundtripTest {
    @Test
    fun `bridge helpers roundtrip loaded save and export commands`() {
        val importBuilder = MindmapBridgeImportMessageBuilder()
        val sentMessages = mutableListOf<Map<String, String>>()
        val savedPayloads = mutableListOf<String>()
        val exportedPayloads = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val dispatcher = MindmapBridgeCommandDispatcher(
            log = {},
            notifyError = { errors += it },
            onLoaded = {
                val message = importBuilder.build(
                    extension = "xmind",
                    loadKmText = { error("km branch should not be used") },
                    loadXmindAsKmJson = { """{"root":{"data":{"text":"Roundtrip"}}}""" }
                )
                sentMessages += message
            },
            onSave = { savedPayloads += it.optString("exportData") },
            onExportToImage = { exportedPayloads += it.optString("exportData") }
        )

        dispatcher.dispatch("""{"command":"loaded"}""")
        dispatcher.dispatch("""{"command":"save","exportData":"{\"root\":{\"data\":{\"text\":\"save\"}}}"}""")
        dispatcher.dispatch("""{"command":"exportToImage","exportData":"data:image/png;base64,AAAA"}""")

        assertTrue(errors.isEmpty())
        assertEquals(1, sentMessages.size)
        assertEquals("import", sentMessages.first()["command"])
        assertEquals(".xmind", sentMessages.first()["extName"])
        assertEquals("""{"root":{"data":{"text":"Roundtrip"}}}""", sentMessages.first()["importData"])
        assertEquals("""{"root":{"data":{"text":"save"}}}""", savedPayloads.first())
        assertEquals("data:image/png;base64,AAAA", exportedPayloads.first())

        // Validate outbound message is still safe for JS bridge injection path.
        val encoded = MindmapBridgeMessageCodec.encodeMessage(sentMessages.first())
        val decoded = String(java.util.Base64.getDecoder().decode(encoded))
        val json = JSONObject(decoded)
        assertEquals("import", json.getString("command"))
    }
}
