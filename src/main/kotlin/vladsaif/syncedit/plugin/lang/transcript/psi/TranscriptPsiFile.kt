package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import vladsaif.syncedit.plugin.lang.transcript.TranscriptModel

class TranscriptPsiFile(val model: TranscriptModel, viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptLanguage) {

    override fun getFileType() = TranscriptFileType

    override fun toString() = "Transcript file"



}