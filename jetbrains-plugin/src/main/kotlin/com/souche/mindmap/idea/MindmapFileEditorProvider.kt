package com.souche.mindmap.idea

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MindmapFileEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        val extension = file.extension ?: return false
        return extension == "km" || extension == "xmind"
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return MindmapFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = "mindmap.file.editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
