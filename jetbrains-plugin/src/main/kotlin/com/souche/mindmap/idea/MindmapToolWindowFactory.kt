package com.souche.mindmap.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery

class MindmapToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (!JBCefApp.isSupported()) {
            val panel = javax.swing.JPanel().apply {
                add(javax.swing.JLabel("JCEF is not supported in this IDE/runtime."))
            }
            val content = ContentFactory.getInstance().createContent(panel, "Mindmap", false)
            toolWindow.contentManager.addContent(content)
            return
        }

        val browser = JBCefBrowser()
        val state = MindmapProjectState.getInstance(project)
        state.browser = browser

        val bridge = MindmapBridge(project, browser)
        state.bridge = bridge
        val query = JBCefJSQuery.create(browser).apply {
            addHandler { payload ->
                bridge.onMessage(payload)
                null
            }
        }

        browser.loadHTML(MindmapHtml.page(query.inject("payload")))
        val content = ContentFactory.getInstance().createContent(browser.component, "Mindmap", false)
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Mindmap"
    }
}
