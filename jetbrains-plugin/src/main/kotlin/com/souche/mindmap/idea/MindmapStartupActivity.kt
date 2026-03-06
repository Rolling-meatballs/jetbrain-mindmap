package com.souche.mindmap.idea

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile

class MindmapStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val state = MindmapProjectState.getInstance(project)

        FileEditorManager.getInstance(project).selectedFiles.firstOrNull()?.let { file ->
            if (isSupported(file)) {
                state.currentFile = file
            }
        }

        project.messageBus.connect(project).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    if (!isSupported(file)) return

                    state.currentFile = file
                    state.bridge?.reloadCurrentFile()
                }
            }
        )
    }

    private fun isSupported(file: VirtualFile): Boolean {
        val extension = file.extension ?: return false
        return extension == "km" || extension == "xmind"
    }
}
