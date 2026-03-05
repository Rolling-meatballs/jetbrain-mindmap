package com.souche.mindmap.idea

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64

class MindmapBridge(
    private val project: Project,
    private val browser: JBCefBrowser,
    private val fileProvider: (() -> VirtualFile?)? = null
) {
    private val xmindConverter = XmindConverter()

    fun onMessage(payload: String?): String {
        if (payload.isNullOrBlank()) return ""
        val message = JSONObject(payload)
        when (message.optString("command")) {
            "loaded" -> reloadCurrentFile()
            "save" -> handleSave(message)
            "exportToImage" -> handleExportToImage(message)
        }
        return ""
    }

    fun reloadCurrentFile() {
        val file = resolveCurrentFile() ?: return

        val importData = if (file.extension == "xmind") {
            runCatching {
                xmindConverter.convertToKmJson(file)
            }.onFailure {
                notifyError("Failed to parse ${file.name}. ${it.message ?: "Unknown error"}")
            }.getOrDefault("{}")
        } else {
            runReadAction { VfsUtil.loadText(file) }
        }

        sendToWeb(
            mapOf(
                "command" to "import",
                "importData" to importData,
                "extName" to ".${file.extension.orEmpty()}"
            )
        )
    }

    private fun handleSave(message: JSONObject) {
        val file = resolveCurrentFile() ?: return
        val exportData = message.optString("exportData")
        val targetFile = resolveSaveTarget(file)

        WriteCommandAction.runWriteCommandAction(project) {
            VfsUtil.saveText(targetFile, exportData)
            targetFile.refresh(false, false)
        }

        notifyInfo("Saved ${targetFile.name}")
    }

    private fun handleExportToImage(message: JSONObject) {
        val file = resolveCurrentFile() ?: return
        val exportData = message.optString("exportData")
        if (exportData.isBlank()) {
            notifyError("No image payload received.")
            return
        }

        val targetFile = resolvePngTarget(file)
        val rawBase64 = exportData.replace(Regex("^data:image/\\w+;base64,"), "")
        val bytes = runCatching { Base64.getDecoder().decode(rawBase64) }
            .onFailure { notifyError("Invalid image payload.") }
            .getOrNull() ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            targetFile.setBinaryContent(bytes)
            targetFile.refresh(false, false)
        }

        notifyInfo("Exported ${targetFile.name}")
    }

    private fun resolveSaveTarget(file: VirtualFile): VirtualFile {
        if (file.extension != "xmind") return file
        val parent = file.parent ?: return file
        val kmName = "${file.nameWithoutExtension}.km"
        return parent.findChild(kmName) ?: parent.createChildData(this, kmName)
    }

    private fun resolveCurrentFile(): VirtualFile? {
        return fileProvider?.invoke() ?: MindmapProjectState.getInstance(project).currentFile
    }

    private fun resolvePngTarget(file: VirtualFile): VirtualFile {
        val parent = file.parent ?: return file
        val pngName = "${file.nameWithoutExtension}.png"
        return parent.findChild(pngName) ?: parent.createChildData(this, pngName)
    }

    private fun sendToWeb(message: Map<String, String>) {
        val json = JSONObject(message).toString()
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        browser.cefBrowser.executeJavaScript(
            """
            (function() {
              var binary = atob('$encoded');
              var bytes = new Uint8Array(binary.length);
              for (var i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
              }
              var data = JSON.parse(new TextDecoder('utf-8').decode(bytes));
              window.dispatchEvent(new MessageEvent('message', { data: data }));
              if (window.mindmapHost && typeof window.mindmapHost.onMessage === 'function') {
                window.mindmapHost.onMessage(JSON.stringify(data));
              }
            })();
            """.trimIndent(),
            browser.cefBrowser.url,
            0
        )
    }

    private fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyError(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }
}
