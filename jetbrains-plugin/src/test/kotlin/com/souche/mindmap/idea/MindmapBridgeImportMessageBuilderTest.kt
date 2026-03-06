package com.souche.mindmap.idea

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindmapBridgeImportMessageBuilderTest {
    private val builder = MindmapBridgeImportMessageBuilder()

    @Test
    fun `xmind extension uses xmind converter branch`() {
        var kmCalled = false
        var xmindCalled = false

        val message = builder.build(
            extension = "xmind",
            loadKmText = {
                kmCalled = true
                "km"
            },
            loadXmindAsKmJson = {
                xmindCalled = true
                """{"root":{}}"""
            }
        )

        assertEquals("import", message["command"])
        assertEquals("""{"root":{}}""", message["importData"])
        assertEquals(".xmind", message["extName"])
        assertTrue(xmindCalled)
        assertTrue(!kmCalled)
    }

    @Test
    fun `non-xmind extension uses plain text branch`() {
        var kmCalled = false
        var xmindCalled = false

        val message = builder.build(
            extension = "km",
            loadKmText = {
                kmCalled = true
                """{"km":"data"}"""
            },
            loadXmindAsKmJson = {
                xmindCalled = true
                "xmind"
            }
        )

        assertEquals("""{"km":"data"}""", message["importData"])
        assertEquals(".km", message["extName"])
        assertTrue(kmCalled)
        assertTrue(!xmindCalled)
    }
}
