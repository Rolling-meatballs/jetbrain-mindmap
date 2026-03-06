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

    private fun JSONObject.rootRelations(): JSONArray {
        val root = optJSONObject("root")
        assertNotNull(root)
        val data = root.optJSONObject("data")
        assertNotNull(data)
        return data.optJSONArray("xmindRelations") ?: JSONArray()
    }
}
