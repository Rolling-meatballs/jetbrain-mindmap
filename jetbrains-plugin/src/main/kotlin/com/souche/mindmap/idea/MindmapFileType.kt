package com.souche.mindmap.idea

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import javax.swing.Icon

object MindmapFileType : LanguageFileType(PlainTextLanguage.INSTANCE) {
    override fun getName(): String = "Mindmap"

    override fun getDescription(): String = "Mindmap file (.km, .xmind)"

    override fun getDefaultExtension(): String = "km"

    override fun getIcon(): Icon? = null
}
