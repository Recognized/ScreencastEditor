package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.util.Disposer
import com.intellij.psi.FileViewProvider
import vladsaif.syncedit.plugin.MultimediaModel

class TranscriptPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptLanguage) {
  val model: MultimediaModel?
    get() = MultimediaModel.getOrCreate(project, viewProvider.virtualFile)

  override fun getFileType() = TranscriptFileType

  override fun toString() = "Transcript file"

  override fun delete() {
    model?.let { Disposer.dispose(it) }
  }
}