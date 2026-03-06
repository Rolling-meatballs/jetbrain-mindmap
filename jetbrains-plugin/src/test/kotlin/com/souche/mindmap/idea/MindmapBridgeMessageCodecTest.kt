package com.souche.mindmap.idea

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindmapBridgeMessageCodecTest {
    @Test
    fun `encode message keeps utf8 payload`() {
        val encoded = MindmapBridgeMessageCodec.encodeMessage(
            mapOf(
                "command" to "import",
                "importData" to """{"text":"中文✅"}""",
                "extName" to ".xmind"
            )
        )

        val decoded = String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
        val json = JSONObject(decoded)
        assertEquals("import", json.getString("command"))
        assertEquals("""{"text":"中文✅"}""", json.getString("importData"))
        assertEquals(".xmind", json.getString("extName"))
    }

    @Test
    fun `injection script contains encoded payload and bridge hooks`() {
        val script = MindmapBridgeMessageCodec.buildInjectionScript("YWJj")
        assertTrue(script.contains("atob('YWJj')"))
        assertTrue(script.contains("new TextDecoder('utf-8')"))
        assertTrue(script.contains("window.dispatchEvent(new MessageEvent('message'"))
        assertTrue(script.contains("window.mindmapHost.onMessage(JSON.stringify(data))"))
    }
}
