package vladsaif.syncedit.plugin.lang.transcript

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptViewFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptViewLanguage

class TranscriptFileEditorProvider : PsiAwareTextEditorProvider() {

    override fun getEditorTypeId() = TYPE_ID

    override fun accept(project: Project, file: VirtualFile): Boolean {
        val accepted = isTranscriptExtension(file.extension) &&
                file.isValid &&
                !file.isDirectory &&
                !file.fileType.isBinary
        val ret = accepted  && try {
            TranscriptData.createFrom(file.inputStream)
            true
        } catch (ex: Throwable) {
            false
        }
        println(ret)
        return ret
    }

    private fun isTranscriptExtension(ext: String?) = ext == "transcript"

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val text = try {
            TranscriptData.createFrom(file.inputStream).text
        } catch (ex: Throwable) {
            "[corrupted xml]"
        }
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                file.nameWithoutExtension,
                TranscriptViewLanguage,
                text,
                true,
                true,
                false,
                file
        )
        val editor = EditorFactory.getInstance().createEditor(
                psiFile.viewProvider.document!!,
                project,
                TranscriptViewFileType,
                false
        )
        return PsiAwareTextEditorProvider.getInstance().getTextEditor(editor)
    }

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        const val TYPE_ID = "transcript-file-editor"
    }
}