package com.souche.mindmap.idea

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser

@Service(Service.Level.PROJECT)
class MindmapProjectState {
    var browser: JBCefBrowser? = null
    var bridge: MindmapBridge? = null
    var currentFile: VirtualFile? = null

    companion object {
        fun getInstance(project: Project): MindmapProjectState = project.service()
    }
}
