package com.souche.mindmap.idea

internal class MindmapBridgeImportMessageBuilder {
    fun build(
        extension: String?,
        loadKmText: () -> String,
        loadXmindAsKmJson: () -> String
    ): Map<String, String> {
        val importData = if (extension == "xmind") {
            loadXmindAsKmJson()
        } else {
            loadKmText()
        }

        return mapOf(
            "command" to "import",
            "importData" to importData,
            "extName" to ".${extension.orEmpty()}"
        )
    }
}
