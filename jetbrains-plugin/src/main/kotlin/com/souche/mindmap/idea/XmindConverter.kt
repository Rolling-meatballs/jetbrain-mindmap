package com.souche.mindmap.idea

import com.intellij.openapi.vfs.VirtualFile
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XmindConverter {
    fun convertToKmJson(file: VirtualFile): String {
        val zipData = readZipEntries(file)

        zipData["content.json"]?.let { bytes ->
            return fromContentJson(String(bytes, StandardCharsets.UTF_8)).toString()
        }

        zipData["content.xml"]?.let { bytes ->
            return fromContentXml(bytes).toString()
        }

        return EMPTY_DOC.toString()
    }

    private fun readZipEntries(file: VirtualFile): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        file.inputStream.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val key = entry.name.substringAfterLast('/')
                        entries[key] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return entries
    }

    private fun fromContentJson(content: String): JSONObject {
        val sheets = JSONArray(content)
        if (sheets.length() == 0) return EMPTY_DOC

        val primarySheet = sheets.optJSONObject(0) ?: return EMPTY_DOC
        val rootTopic = primarySheet.optJSONObject("rootTopic") ?: return EMPTY_DOC

        return wrapKm(topicFromJson(rootTopic))
    }

    private fun topicFromJson(topic: JSONObject): JSONObject {
        val node = JSONObject()
        node.put("data", JSONObject().apply {
            put("id", topic.optString("id", UUID.randomUUID().toString()))
            put("text", topic.optString("title", ""))
            put("created", System.currentTimeMillis())
        })

        val children = JSONArray()
        val attached = topic.optJSONObject("children")?.optJSONArray("attached") ?: JSONArray()
        for (index in 0 until attached.length()) {
            val childTopic = attached.optJSONObject(index) ?: continue
            children.put(topicFromJson(childTopic))
        }

        node.put("children", children)
        return node
    }

    private fun fromContentXml(content: ByteArray): JSONObject {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(content))
        val sheets = document.getElementsByTagName("sheet")
        if (sheets.length == 0) return EMPTY_DOC

        val firstSheet = sheets.item(0) as? Element ?: return EMPTY_DOC
        val topics = firstSheet.getElementsByTagName("topic")
        if (topics.length == 0) return EMPTY_DOC

        val rootTopic = topics.item(0) as? Element ?: return EMPTY_DOC
        return wrapKm(topicFromXml(rootTopic))
    }

    private fun topicFromXml(topic: Element): JSONObject {
        val node = JSONObject()
        node.put("data", JSONObject().apply {
            put("id", topic.getAttribute("id").ifBlank { UUID.randomUUID().toString() })
            put("text", directChildText(topic, "title"))
            put("created", System.currentTimeMillis())
        })

        val children = JSONArray()
        val childrenElement = directChild(topic, "children")
        val topicsElement = childrenElement?.let { findAttachedTopics(it) }

        if (topicsElement != null) {
            val childNodes = topicsElement.getElementsByTagName("topic")
            for (index in 0 until childNodes.length) {
                val childTopic = childNodes.item(index) as? Element ?: continue
                if (childTopic.parentNode == topicsElement) {
                    children.put(topicFromXml(childTopic))
                }
            }
        }

        node.put("children", children)
        return node
    }

    private fun findAttachedTopics(children: Element): Element? {
        val topics = children.getElementsByTagName("topics")
        for (index in 0 until topics.length) {
            val element = topics.item(index) as? Element ?: continue
            if (element.getAttribute("type") == "attached") {
                return element
            }
        }
        return null
    }

    private fun directChild(parent: Element, name: String): Element? {
        val nodes = parent.childNodes
        for (index in 0 until nodes.length) {
            val child = nodes.item(index) as? Element ?: continue
            if (child.tagName == name) {
                return child
            }
        }
        return null
    }

    private fun directChildText(parent: Element, name: String): String {
        val element = directChild(parent, name)
        return element?.textContent?.trim().orEmpty()
    }

    private fun wrapKm(root: JSONObject): JSONObject {
        return JSONObject().apply {
            put("root", root)
            put("template", "right")
            put("theme", "fresh-blue-compat")
            put("version", "1.4.43")
        }
    }

    companion object {
        private val EMPTY_DOC = JSONObject().apply {
            put("root", JSONObject())
            put("template", "right")
            put("theme", "fresh-blue-compat")
            put("version", "1.4.43")
        }
    }
}
