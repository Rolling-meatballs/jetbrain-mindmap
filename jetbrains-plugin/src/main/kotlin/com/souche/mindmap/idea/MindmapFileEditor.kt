package com.souche.mindmap.idea

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MindmapFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor, Disposable {
    private val fallbackPanel = JPanel().apply {
        add(JLabel("JCEF is not supported in this IDE/runtime."))
    }

    private var browser: JBCefBrowser? = null
    private var bridge: MindmapBridge? = null
    private var query: JBCefJSQuery? = null

    init {
        if (JBCefApp.isSupported()) {
            val editorBrowser = JBCefBrowser()
            val editorBridge = MindmapBridge(project, editorBrowser) { file }
            val editorQuery = JBCefJSQuery.create(editorBrowser).apply {
                addHandler { payload ->
                    editorBridge.onMessage(payload)
                    null
                }
            }

            val webUi = MindmapWebUiLoader(project).load(editorQuery.inject("payload"))
            editorBrowser.loadHTML(webUi.html)

            if (webUi.source == MindmapWebUiLoader.Source.FALLBACK) {
                val message = webUi.issues.joinToString(" ")
                notifyWarning("Using fallback UI. $message")
            }

            browser = editorBrowser
            bridge = editorBridge
            query = editorQuery
        } else {
            notifyWarning("JCEF is not supported in this IDE/runtime.")
        }
    }

    override fun getComponent(): JComponent = browser?.component ?: fallbackPanel

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun getName(): String = "Mindmap"

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun selectNotify() {
        val state = MindmapProjectState.getInstance(project)
        state.currentFile = file
        bridge?.reloadCurrentFile()
    }

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun dispose() {
        query?.let { Disposer.dispose(it) }
        query = null
        browser?.dispose()
        browser = null
        bridge = null
    }

    private fun notifyWarning(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }
}
