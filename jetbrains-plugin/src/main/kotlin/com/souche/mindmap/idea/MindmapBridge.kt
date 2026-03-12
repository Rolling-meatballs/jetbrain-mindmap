package com.souche.mindmap.idea

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.charset.StandardCharsets
import java.util.Base64

class MindmapBridge(
    private val project: Project,
    private val browser: JBCefBrowser,
    private val fileProvider: (() -> VirtualFile?)? = null
) {
    private val xmindConverter = XmindConverter()
    private val importMessageBuilder = MindmapBridgeImportMessageBuilder()
    private val commandDispatcher = MindmapBridgeCommandDispatcher(
        log = ::appendBridgeLog,
        notifyError = ::notifyError,
        onLoaded = ::reloadCurrentFile,
        onSave = ::handleSave,
        onExportToImage = ::handleExportToImage
    )

    init {
        appendBridgeLog("bridge_initialized")
    }

    fun onMessage(payload: String?): String {
        commandDispatcher.dispatch(payload)
        return ""
    }

    fun reloadCurrentFile() {
        val file = resolveCurrentFile() ?: return

        val message = importMessageBuilder.build(
            extension = file.extension,
            loadKmText = { ReadAction.compute<String, RuntimeException> { VfsUtil.loadText(file) } },
            loadXmindAsKmJson = {
                runCatching { xmindConverter.convertToKmJson(file) }
                    .onFailure { notifyError("Failed to parse ${file.name}. ${it.message ?: "Unknown error"}") }
                    .getOrDefault("{}")
                    .also(::notifyRelationImportFallback)
            }
        )

        sendToWeb(message)
    }

    private fun handleSave(message: JSONObject) {
        val file = requireCurrentFile("save") ?: return
        val exportData = message.optString("exportData")
        appendBridgeLog("save_queued source=${file.path}")
        ApplicationManager.getApplication().invokeLater {
            runCatching {
                WriteCommandAction.runWriteCommandAction(project) {
                    val targetFile = if (file.extension != "xmind") {
                        file
                    } else {
                        val parent = file.parent ?: error("Cannot resolve parent directory for ${file.name}")
                        val kmName = "${file.nameWithoutExtension}.km"
                        parent.findChild(kmName) ?: parent.createChildData(this, kmName)
                    }
                    VfsUtil.saveText(targetFile, exportData)
                    targetFile.refresh(false, false)
                    appendBridgeLog("saved path=${targetFile.path} bytes=${exportData.toByteArray(StandardCharsets.UTF_8).size}")
                }
            }.onSuccess {
                notifyInfo("Saved ${file.name}")
            }.onFailure {
                notifyError("Save failed for ${file.name}: ${it.message ?: "Unknown error"}")
            }
        }
    }

    private fun handleExportToImage(message: JSONObject) {
        val file = requireCurrentFile("PNG export") ?: return
        val exportData = message.optString("exportData")
        if (exportData.isBlank()) {
            notifyError("No image payload received.")
            return
        }

        val rawBase64 = exportData.replace(Regex("^data:image/\\w+;base64,"), "")
        val bytes = runCatching { Base64.getDecoder().decode(rawBase64) }
            .onFailure { notifyError("Invalid image payload.") }
            .getOrNull() ?: return

        appendBridgeLog("export_queued source=${file.path}")
        ApplicationManager.getApplication().invokeLater {
            runCatching {
                WriteCommandAction.runWriteCommandAction(project) {
                    val parent = file.parent ?: error("Cannot resolve parent directory for ${file.name}")
                    val pngName = "${file.nameWithoutExtension}.png"
                    val targetFile = parent.findChild(pngName) ?: parent.createChildData(this, pngName)
                    targetFile.setBinaryContent(bytes)
                    targetFile.refresh(false, false)
                    appendBridgeLog("exported path=${targetFile.path} bytes=${bytes.size}")
                }
            }.onSuccess {
                notifyInfo("Exported ${file.nameWithoutExtension}.png")
            }.onFailure {
                notifyError("Export failed for ${file.name}: ${it.message ?: "Unknown error"}")
            }
        }
    }

    private fun resolveCurrentFile(): VirtualFile? {
        return fileProvider?.invoke() ?: MindmapProjectState.getInstance(project).currentFile
    }

    private fun requireCurrentFile(action: String): VirtualFile? {
        val file = resolveCurrentFile()
        if (file == null) {
            notifyError("No current file selected for $action.")
        }
        return file
    }


    private fun sendToWeb(message: Map<String, String>) {
        val encoded = MindmapBridgeMessageCodec.encodeMessage(message)
        browser.cefBrowser.executeJavaScript(
            MindmapBridgeMessageCodec.buildInjectionScript(encoded),
            browser.cefBrowser.url,
            0
        )
    }

    private fun notifyRelationImportFallback(importData: String) {
        val relationCount = runCatching {
            JSONObject(importData)
                .optJSONObject("root")
                ?.optJSONObject("data")
                ?.optJSONArray("xmindRelations")
                ?.length() ?: 0
        }.getOrDefault(0)

        if (relationCount > 0) {
            notifyInfo("Imported $relationCount relation(s) as metadata (xmindRelations); current web UI does not render relation lines yet.")
        }
    }

    private fun notifyInfo(content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyError(content: String) {
        appendBridgeLog("error=$content")
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Mindmap Notifications")
            .createNotification(content, NotificationType.ERROR)
            .notify(project)
    }

    private fun appendBridgeLog(line: String) {
        val record = "${System.currentTimeMillis()} | $line\n"

        val candidatePaths = mutableListOf(Paths.get(PathManager.getLogPath(), "mindmap-bridge.log"))
        project.basePath?.let { candidatePaths.add(Paths.get(it, ".mindmap-bridge.log")) }

        for (path in candidatePaths) {
            runCatching {
                Files.writeString(
                    path,
                    record,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
                )
            }
        }
    }
}
