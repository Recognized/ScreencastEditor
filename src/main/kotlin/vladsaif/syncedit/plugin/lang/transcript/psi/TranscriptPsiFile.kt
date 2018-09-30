package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.util.Disposer
import com.intellij.psi.FileViewProvider
import vladsaif.syncedit.plugin.ScreencastFile

class TranscriptPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptLanguage) {
  val model: ScreencastFile?
    get() = virtualFile.getUserData(ScreencastFile.KEY)

  override fun getFileType() = TranscriptFileType

  override fun toString() = "Transcript file"

  override fun delete() {
    virtualFile.putUserData(ScreencastFile.KEY, null)
    model?.let { Disposer.dispose(it) }
  }
}