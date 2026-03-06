package com.souche.mindmap.idea

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
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
        val query = JBCefJSQuery.create(browser as JBCefBrowserBase).apply {
            addHandler { payload ->
                bridge.onMessage(payload)
                null
            }
        }

        val webUi = MindmapWebUiLoader(project).load(query.inject("payload"))
        browser.loadHTML(webUi.html)
        if (webUi.source == MindmapWebUiLoader.Source.FALLBACK) {
            val message = webUi.issues.joinToString(" ")
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Mindmap Notifications")
                .createNotification("Using fallback UI. $message", NotificationType.WARNING)
                .notify(project)
        }
        val content = ContentFactory.getInstance().createContent(browser.component, "Mindmap", false)
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val TOOL_WINDOW_ID = "Mindmap"
    }
}
