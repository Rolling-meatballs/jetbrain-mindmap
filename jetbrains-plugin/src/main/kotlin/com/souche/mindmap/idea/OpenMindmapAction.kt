package com.souche.mindmap.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.ToolWindowManager

class OpenMindmapAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: project?.let { FileEditorManager.getInstance(it).selectedFiles.firstOrNull() }
        e.presentation.isEnabledAndVisible = file != null && isSupported(file.extension)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            ?: return

        if (!isSupported(file.extension)) return

        val state = MindmapProjectState.getInstance(project)
        state.currentFile = file
        ToolWindowManager.getInstance(project)
            .getToolWindow(MindmapToolWindowFactory.TOOL_WINDOW_ID)
            ?.show {
                state.bridge?.reloadCurrentFile()
            }
    }

    private fun isSupported(extension: String?): Boolean {
        return extension == "km" || extension == "xmind"
    }
}
