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
    private data class XmindRelation(
        val fromId: String,
        val toId: String,
        val title: String
    )

    fun convertToKmJson(file: VirtualFile): String {
        return convertToKmJson(readZipEntries(file))
    }

    internal fun convertToKmJson(zipData: Map<String, ByteArray>): String {
        val normalizedEntries = zipData.mapKeys { it.key.substringAfterLast('/') }

        normalizedEntries["content.json"]?.let { bytes ->
            return fromContentJson(String(bytes, StandardCharsets.UTF_8)).toString()
        }

        normalizedEntries["content.xml"]?.let { bytes ->
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
        val km = wrapKm(topicFromJson(rootTopic))
        applyRelations(km, extractJsonRelations(primarySheet))
        return km
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
        val km = wrapKm(topicFromXml(rootTopic))
        applyRelations(km, extractXmlRelations(firstSheet))
        return km
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
        applyJsonStyle(topic, data)
        applyJsonImage(topic, data)

        return data
    }

    private fun buildDataFromXml(topic: Element): JSONObject {
        val data = JSONObject()
        data.put("id", topic.getAttribute("id").ifBlank { UUID.randomUUID().toString() })
        data.put("text", directChildText(topic, "title"))
        data.put("created", System.currentTimeMillis())

        firstXmlAttribute(topic, "xlink:href", "href")?.takeIf { it.isNotBlank() }?.let { data.put("hyperlink", it) }
        extractXmlNote(topic).takeIf { it.isNotBlank() }?.let { data.put("note", it) }
        extractXmlLabels(topic).takeIf { it.length() > 0 }?.let { data.put("resource", it) }
        extractXmlPriority(topic)?.let { data.put("priority", it) }
        extractXmlProgress(topic)?.let { data.put("progress", it) }
        applyXmlStyle(topic, data)
        applyXmlImage(topic, data)

        return data
    }

    private fun applyJsonStyle(topic: JSONObject, data: JSONObject) {
        val styleSources = buildList {
            topic.optJSONObject("style")?.let { add(it) }
            topic.optJSONObject("style")?.optJSONObject("properties")?.let { add(it) }
            topic.optJSONObject("titleStyle")?.let { add(it) }
            topic.optJSONObject("topicStyle")?.let { add(it) }
        }

        firstJsonString(styleSources, "fo:color", "textColor", "fontColor", "color")
            ?.let { data.put("color", it) }
        firstJsonString(styleSources, "svg:fill", "fillColor", "fill", "background", "backgroundColor")
            ?.let { data.put("background", it) }
        firstJsonString(styleSources, "fo:font-family", "fontFamily", "font-family")
            ?.let { data.put("font-family", it) }

        firstJsonString(styleSources, "fo:font-size", "fontSize", "font-size")
            ?.let(::parseDimension)
            ?.let { size ->
                if (size > 0) data.put("font-size", size)
            }
    }

    private fun applyJsonImage(topic: JSONObject, data: JSONObject) {
        val image = topic.optJSONObject("image") ?: return
        val url = firstJsonString(listOf(image), "src", "xlink:href", "href", "url") ?: return
        val width = parseDimension(image.opt("width"))
        val height = parseDimension(image.opt("height"))
        // kityminder png export assumes imageSize exists when image exists.
        if (width == null || height == null || width <= 0 || height <= 0) return

        data.put("image", url)
        firstJsonString(listOf(image), "title", "alt")
            ?.let { data.put("imageTitle", it) }
        data.put(
            "imageSize",
            JSONObject().apply {
                put("width", width)
                put("height", height)
            }
        )
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

    private fun applyXmlStyle(topic: Element, data: JSONObject) {
        val styleSources = buildList {
            add(topic)
            directChild(topic, "style")?.let { add(it) }
            directChild(topic, "topic-style")?.let { add(it) }
            directChild(topic, "title")?.let { add(it) }
        }

        firstXmlAttribute(styleSources, "fo:color", "text-color", "textColor", "color")
            ?.let { data.put("color", it) }
        firstXmlAttribute(styleSources, "svg:fill", "fill-color", "fillColor", "fill", "background")
            ?.let { data.put("background", it) }
        firstXmlAttribute(styleSources, "fo:font-family", "font-family", "fontFamily")
            ?.let { data.put("font-family", it) }
        firstXmlAttribute(styleSources, "fo:font-size", "font-size", "fontSize")
            ?.let(::parseDimension)
            ?.let { size ->
                if (size > 0) data.put("font-size", size)
            }
    }

    private fun applyXmlImage(topic: Element, data: JSONObject) {
        val imageElement = firstXmlImageElement(topic) ?: return
        val url = firstXmlAttribute(imageElement, "xhtml:src", "src", "xlink:href", "href") ?: return
        val width = firstXmlAttribute(imageElement, "svg:width", "width")?.let(::parseDimension)
        val height = firstXmlAttribute(imageElement, "svg:height", "height")?.let(::parseDimension)
        if (width == null || height == null || width <= 0 || height <= 0) return

        data.put("image", url)
        firstXmlAttribute(imageElement, "title", "alt")
            ?.let { data.put("imageTitle", it) }
        data.put(
            "imageSize",
            JSONObject().apply {
                put("width", width)
                put("height", height)
            }
        )
    }

    private fun firstJsonString(sources: List<JSONObject>, vararg keys: String): String? {
        for (source in sources) {
            for (key in keys) {
                val value = source.optString(key).trim()
                if (value.isNotEmpty()) {
                    return value
                }
            }
        }
        return null
    }

    private fun firstXmlAttribute(element: Element, vararg keys: String): String? {
        return firstXmlAttribute(listOf(element), *keys)
    }

    private fun firstXmlAttribute(elements: List<Element>, vararg keys: String): String? {
        for (element in elements) {
            for (key in keys) {
                element.getAttribute(key).trim().takeIf { it.isNotEmpty() }?.let { return it }
            }

            val attributes = element.attributes
            for (index in 0 until attributes.length) {
                val node = attributes.item(index) ?: continue
                val attrName = node.nodeName
                val attrValue = node.nodeValue?.trim().orEmpty()
                if (attrValue.isEmpty()) continue

                for (key in keys) {
                    if (attrName.equals(key, ignoreCase = true) || attrName.endsWith(":$key", ignoreCase = true)) {
                        return attrValue
                    }
                }
            }
        }
        return null
    }

    private fun firstXmlImageElement(topic: Element): Element? {
        val nodes = topic.getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val name = element.tagName.lowercase()
            if (name == "img" || name.endsWith(":img")) {
                return element
            }
        }
        return null
    }

    private fun parseDimension(raw: Any?): Int? {
        return when (raw) {
            null -> null
            is Number -> raw.toInt()
            is String -> {
                val match = Regex("""-?\d+(\.\d+)?""").find(raw.trim()) ?: return null
                match.value.toDoubleOrNull()?.toInt()
            }
            else -> raw.toString().toIntOrNull()
        }
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

    private fun extractJsonRelations(sheet: JSONObject): List<XmindRelation> {
        val rawRelations = sheet.optJSONArray("relationships") ?: return emptyList()
        val relations = mutableListOf<XmindRelation>()
        for (index in 0 until rawRelations.length()) {
            val relation = rawRelations.optJSONObject(index) ?: continue
            val fromId = firstNonBlank(
                relation.optString("end1Id"),
                relation.optString("end1"),
                relation.optString("sourceId"),
                relation.optString("source")
            ) ?: continue
            val toId = firstNonBlank(
                relation.optString("end2Id"),
                relation.optString("end2"),
                relation.optString("targetId"),
                relation.optString("target")
            ) ?: continue
            val title = relation.optString("title").trim()
            relations += XmindRelation(fromId, toId, title)
        }
        return relations
    }

    private fun extractXmlRelations(sheet: Element): List<XmindRelation> {
        val relationNodes = sheet.getElementsByTagName("relationship")
        val relations = mutableListOf<XmindRelation>()
        for (index in 0 until relationNodes.length) {
            val relation = relationNodes.item(index) as? Element ?: continue
            val fromId = firstXmlAttribute(relation, "end1", "end1id", "source", "sourceid") ?: continue
            val toId = firstXmlAttribute(relation, "end2", "end2id", "target", "targetid") ?: continue
            val title = firstNonBlank(
                firstXmlAttribute(relation, "title"),
                directChildText(relation, "title")
            ).orEmpty()
            relations += XmindRelation(fromId, toId, title)
        }
        return relations
    }

    private fun applyRelations(km: JSONObject, relations: List<XmindRelation>) {
        if (relations.isEmpty()) return
        val root = km.optJSONObject("root") ?: return
        val rootData = root.optJSONObject("data") ?: JSONObject().also { root.put("data", it) }
        val relationArray = JSONArray()

        relations
            .distinctBy { "${it.fromId}->${it.toId}:${it.title}" }
            .forEach { relation ->
                val relationJson = JSONObject().apply {
                    put("from", relation.fromId)
                    put("to", relation.toId)
                    if (relation.title.isNotBlank()) {
                        put("title", relation.title)
                    }
                }
                relationArray.put(relationJson)
            }

        if (relationArray.length() > 0) {
            // Keep relation data lossless even though current webui does not render cross-topic links.
            rootData.put("xmindRelations", relationArray)
        }
    }

    private fun firstNonBlank(vararg values: String?): String? {
        for (value in values) {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isNotEmpty()) {
                return trimmed
            }
        }
        return null
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
