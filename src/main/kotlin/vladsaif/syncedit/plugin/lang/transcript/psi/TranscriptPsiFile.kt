package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.util.Disposer
import com.intellij.psi.FileViewProvider
import vladsaif.syncedit.plugin.TranscriptModel

class TranscriptPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptLanguage) {
    val model: TranscriptModel?
        get() = TranscriptModel.fileModelMap[viewProvider.virtualFile]

    override fun getFileType() = TranscriptFileType

    override fun toString() = "Transcript file"

    override fun delete() {
        model?.let { Disposer.dispose(it) }
    }
}