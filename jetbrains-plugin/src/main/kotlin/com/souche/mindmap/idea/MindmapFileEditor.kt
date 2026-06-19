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
import com.intellij.ui.jcef.JBCefBrowserBase
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
            val editorQuery = JBCefJSQuery.create(editorBrowser as JBCefBrowserBase).apply {
                addHandler { payload ->
                    editorBridge.onMessage(payload)
                    null
                }
            }

            val jsQueryCall = editorQuery.inject("payload")
            if (System.getProperty("mindmap.react.editor") == "true") {
                ensureMindmapReactSchemeRegistered()
                // Serve index.html via the scheme handler (loadURL) so sub-resource
                // requests (/assets, /vendor) route through it; loadHTML does not.
                // The bridge shim is injected by the handler, keyed by this browser.
                MindmapReactBridgeRegistry.register(editorBrowser.cefBrowser, buildReactShim(jsQueryCall))
                editorBrowser.loadURL("$MINDMAP_REACT_ORIGIN/index.html")
            } else {
                loadWebUi(editorBrowser, jsQueryCall)
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

    override fun getFile(): VirtualFile = file

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
        browser?.let { MindmapReactBridgeRegistry.unregister(it.cefBrowser) }
        browser?.dispose()
        browser = null
        bridge = null
    }

    private fun loadWebUi(jcefBrowser: JBCefBrowser, jsQueryCall: String) {
        val webUi = MindmapWebUiLoader(project).load(jsQueryCall)
        jcefBrowser.loadHTML(webUi.html)
        if (webUi.source == MindmapWebUiLoader.Source.FALLBACK) {
            notifyWarning("Using fallback UI. ${webUi.issues.joinToString(" ")}")
        }
    }

    private fun notifyWarning(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.WARNING)
            .notify(project)
    }
}
