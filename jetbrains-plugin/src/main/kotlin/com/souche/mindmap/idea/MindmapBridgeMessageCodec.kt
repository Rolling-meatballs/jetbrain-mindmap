package com.souche.mindmap.idea

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object MindmapBridgeMessageCodec {
    fun encodeMessage(message: Map<String, String>): String {
        val json = JSONObject(message).toString()
        return Base64.getEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }

    fun buildInjectionScript(encodedPayload: String): String {
        return """
            (function() {
              var binary = atob('$encodedPayload');
              var bytes = new Uint8Array(binary.length);
              for (var i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
              }
              var data = JSON.parse(new TextDecoder('utf-8').decode(bytes));
              window.dispatchEvent(new MessageEvent('message', { data: data }));
              if (window.mindmapHost && typeof window.mindmapHost.onMessage === 'function') {
                window.mindmapHost.onMessage(JSON.stringify(data));
              }
            })();
            """.trimIndent()
    }
}
