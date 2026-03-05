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
        node.put("data", buildDataFromJson(topic))

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
        node.put("data", buildDataFromXml(topic))

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

    private fun buildDataFromJson(topic: JSONObject): JSONObject {
        val data = JSONObject()
        data.put("id", topic.optString("id", UUID.randomUUID().toString()))
        data.put("text", topic.optString("title", ""))
        data.put("created", System.currentTimeMillis())

        topic.optString("href").takeIf { it.isNotBlank() }?.let { data.put("hyperlink", it) }
        extractJsonNote(topic).takeIf { it.isNotBlank() }?.let { data.put("note", it) }
        extractJsonLabels(topic).takeIf { it.length() > 0 }?.let { data.put("resource", it) }
        extractJsonPriority(topic)?.let { data.put("priority", it) }
        extractJsonProgress(topic)?.let { data.put("progress", it) }

        return data
    }

    private fun buildDataFromXml(topic: Element): JSONObject {
        val data = JSONObject()
        data.put("id", topic.getAttribute("id").ifBlank { UUID.randomUUID().toString() })
        data.put("text", directChildText(topic, "title"))
        data.put("created", System.currentTimeMillis())

        topic.getAttribute("xlink:href").takeIf { it.isNotBlank() }?.let { data.put("hyperlink", it) }
        extractXmlNote(topic).takeIf { it.isNotBlank() }?.let { data.put("note", it) }
        extractXmlLabels(topic).takeIf { it.length() > 0 }?.let { data.put("resource", it) }
        extractXmlPriority(topic)?.let { data.put("priority", it) }
        extractXmlProgress(topic)?.let { data.put("progress", it) }

        return data
    }

    private fun extractJsonNote(topic: JSONObject): String {
        val notes = topic.optJSONObject("notes") ?: return ""
        val plain = notes.optJSONObject("plain")
        if (plain != null) {
            return plain.optString("content").trim()
        }
        return notes.optString("realHTML").trim()
    }

    private fun extractJsonLabels(topic: JSONObject): JSONArray {
        val labels = topic.optJSONArray("labels") ?: return JSONArray()
        val resources = JSONArray()
        for (index in 0 until labels.length()) {
            val raw = labels.optString(index).trim()
            if (raw.isNotBlank()) {
                resources.put(raw)
            }
        }
        return resources
    }

    private fun extractJsonPriority(topic: JSONObject): Int? {
        val markers = topic.optJSONArray("markers") ?: return null
        for (index in 0 until markers.length()) {
            val marker = markers.optJSONObject(index) ?: continue
            val markerId = marker.optString("markerId")
            parsePriority(markerId)?.let { return it }
        }
        return null
    }

    private fun extractJsonProgress(topic: JSONObject): Int? {
        val markers = topic.optJSONArray("markers") ?: return null
        for (index in 0 until markers.length()) {
            val marker = markers.optJSONObject(index) ?: continue
            val markerId = marker.optString("markerId")
            parseProgress(markerId)?.let { return it }
        }
        return null
    }

    private fun extractXmlNote(topic: Element): String {
        val notes = directChild(topic, "notes") ?: return ""
        val plain = directChild(notes, "plain")
        if (plain != null) {
            return plain.textContent.trim()
        }
        return notes.textContent.trim()
    }

    private fun extractXmlLabels(topic: Element): JSONArray {
        val labels = directChild(topic, "labels") ?: return JSONArray()
        val resources = JSONArray()
        val labelNodes = labels.getElementsByTagName("label")
        for (index in 0 until labelNodes.length) {
            val value = labelNodes.item(index)?.textContent?.trim().orEmpty()
            if (value.isNotBlank()) {
                resources.put(value)
            }
        }
        return resources
    }

    private fun extractXmlPriority(topic: Element): Int? {
        val refs = topic.getElementsByTagName("marker-ref")
        for (index in 0 until refs.length) {
            val marker = refs.item(index) as? Element ?: continue
            parsePriority(marker.getAttribute("marker-id"))?.let { return it }
        }
        return null
    }

    private fun extractXmlProgress(topic: Element): Int? {
        val refs = topic.getElementsByTagName("marker-ref")
        for (index in 0 until refs.length) {
            val marker = refs.item(index) as? Element ?: continue
            parseProgress(marker.getAttribute("marker-id"))?.let { return it }
        }
        return null
    }

    private fun parsePriority(markerId: String): Int? {
        val match = Regex("""priority-(\d+)""").find(markerId) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun parseProgress(markerId: String): Int? {
        return when (markerId) {
            "task-start" -> 1
            "task-quarter" -> 2
            "task-half" -> 3
            "task-3quar" -> 4
            "task-done" -> 5
            else -> Regex("""task-(\d+)""").find(markerId)?.groupValues?.get(1)?.toIntOrNull()
        }
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
