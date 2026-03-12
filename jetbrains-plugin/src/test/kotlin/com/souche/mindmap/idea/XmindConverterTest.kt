package com.souche.mindmap.idea

import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XmindConverterTest {
    private val converter = XmindConverter()

    @Test
    fun `imports relations from content json`() {
        val content = """
            [
              {
                "rootTopic": {
                  "id": "root",
                  "title": "Root",
                  "children": {
                    "attached": [
                      {"id": "n1", "title": "Node 1"},
                      {"id": "n2", "title": "Node 2"}
                    ]
                  }
                },
                "relationships": [
                  {"end1Id": "n1", "end2Id": "n2", "title": "depends on"},
                  {"end1Id": "n1", "end2Id": "n2", "title": "depends on"}
                ]
              }
            ]
        """.trimIndent()

        val km = JSONObject(
            converter.convertToKmJson(
                mapOf("content.json" to content.toByteArray())
            )
        )

        val relations = km.rootRelations()
        assertEquals(1, relations.length(), "duplicate relationships should be deduplicated")
        assertEquals("n1", relations.getJSONObject(0).getString("from"))
        assertEquals("n2", relations.getJSONObject(0).getString("to"))
        assertEquals("depends on", relations.getJSONObject(0).getString("title"))
    }

    @Test
    fun `imports relations from content xml`() {
        val xml = """
            <xmap-content>
              <sheet>
                <topic id="root">
                  <title>Root</title>
                  <children>
                    <topics type="attached">
                      <topic id="n1"><title>Node 1</title></topic>
                      <topic id="n2"><title>Node 2</title></topic>
                    </topics>
                  </children>
                </topic>
                <relationship end1="n1" end2="n2" title="references"/>
              </sheet>
            </xmap-content>
        """.trimIndent()

        val km = JSONObject(
            converter.convertToKmJson(
                mapOf("content.xml" to xml.toByteArray())
            )
        )

        val relations = km.rootRelations()
        assertEquals(1, relations.length())
        assertEquals("n1", relations.getJSONObject(0).getString("from"))
        assertEquals("n2", relations.getJSONObject(0).getString("to"))
        assertEquals("references", relations.getJSONObject(0).getString("title"))
    }

    @Test
    fun `ignores incomplete relations`() {
        val content = """
            [
              {
                "rootTopic": {"id": "root", "title": "Root"},
                "relationships": [
                  {"end1Id": "n1"},
                  {"end2Id": "n2"},
                  {"sourceId": "n1", "targetId": "n2"}
                ]
              }
            ]
        """.trimIndent()

        val km = JSONObject(
            converter.convertToKmJson(
                mapOf("content.json" to content.toByteArray())
            )
        )

        val relations = km.rootRelations()
        assertEquals(1, relations.length())
        assertEquals("n1", relations.getJSONObject(0).getString("from"))
        assertEquals("n2", relations.getJSONObject(0).getString("to"))
        assertTrue(!relations.getJSONObject(0).has("title"))
    }

    @Test
    fun `embeds content json images from xmind resources`() {
        val content = """
            [
              {
                "rootTopic": {
                  "id": "root",
                  "title": "Root",
                  "image": {
                    "src": "resources/sample.png",
                    "width": "120",
                    "height": "80"
                  }
                }
              }
            ]
        """.trimIndent()

        val km = JSONObject(
            converter.convertToKmJson(
                mapOf(
                    "content.json" to content.toByteArray(),
                    "resources/sample.png" to byteArrayOf(1, 2, 3, 4)
                )
            )
        )

        val image = km.rootData().getString("image")
        assertTrue(image.startsWith("data:image/png;base64,"))
        assertEquals(120, km.rootData().getJSONObject("imageSize").getInt("width"))
        assertEquals(80, km.rootData().getJSONObject("imageSize").getInt("height"))
    }

    @Test
    fun `embeds content xml images from xmind resources`() {
        val xml = """
            <xmap-content>
              <sheet>
                <topic id="root">
                  <title>Root</title>
                  <xhtml:img xhtml:src="resources/sample.png" svg:width="64" svg:height="32"/>
                </topic>
              </sheet>
            </xmap-content>
        """.trimIndent()

        val km = JSONObject(
            converter.convertToKmJson(
                mapOf(
                    "content.xml" to xml.toByteArray(),
                    "resources/sample.png" to byteArrayOf(9, 8, 7, 6)
                )
            )
        )

        val image = km.rootData().getString("image")
        assertTrue(image.startsWith("data:image/png;base64,"))
        assertEquals(64, km.rootData().getJSONObject("imageSize").getInt("width"))
        assertEquals(32, km.rootData().getJSONObject("imageSize").getInt("height"))
    }

    private fun JSONObject.rootData(): JSONObject {
        val root = optJSONObject("root")
        assertNotNull(root)
        val data = root.optJSONObject("data")
        assertNotNull(data)
        return data
    }

    private fun JSONObject.rootRelations(): JSONArray {
        val root = optJSONObject("root")
        assertNotNull(root)
        val data = root.optJSONObject("data")
        assertNotNull(data)
        return data.optJSONArray("xmindRelations") ?: JSONArray()
    }
}
