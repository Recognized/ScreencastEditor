package vladsaif.syncedit.plugin.transcript

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory

class TranscriptFileEditorProvider : FileEditorProvider {

    override fun getEditorTypeId(): String {
        TODO("not implemented")
    }

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return isTranscriptExtension(file.extension) && file.isValid
    }

    private fun isTranscriptExtension(ext: String?) = ext == "transcript"

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(file.name, XmlFileType.INSTANCE, "hello")
        return EditorFactory.getInstance().createEditor(PsiDocumentManager.getInstance(project).getDocument(psiFile)!!) as FileEditor
    }

    override fun getPolicy() = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    companion object {
        const val TYPE_ID = "transcript-file-editor"
    }
}