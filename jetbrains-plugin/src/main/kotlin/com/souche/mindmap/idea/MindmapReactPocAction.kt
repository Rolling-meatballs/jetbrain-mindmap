package com.souche.mindmap.idea

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.Dimension
import javax.swing.JComponent

// PoC entry point: opens the React build inside a real JCEF browser via the
// http scheme handler, proving ES modules render in JCEF (Chromium 137).
// Isolated from the shipping file:// webui editor path on purpose.
class MindmapReactPocAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (!JBCefApp.isSupported()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Mindmap Notifications")
                .createNotification("JCEF is not supported in this IDE/runtime.", NotificationType.WARNING)
                .notify(project)
            return
        }

        ensureMindmapReactSchemeRegistered()

        val browser = JBCefBrowser.createBuilder()
            .setUrl("$MINDMAP_REACT_ORIGIN/index.html")
            .build()

        val dialog = object : DialogWrapper(project, true) {
            init {
                title = "Mindmap React PoC — JCEF http scheme handler"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val component = browser.component
                component.preferredSize = Dimension(960, 680)
                return component
            }

            override fun dispose() {
                browser.dispose()
                super.dispose()
            }
        }
        dialog.show()
    }
}
