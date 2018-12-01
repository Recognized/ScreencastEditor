package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.util.Disposer
import com.intellij.psi.FileViewProvider
import vladsaif.syncedit.plugin.model.Screencast

class TranscriptPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptLanguage) {
  val model: Screencast?
    get() = virtualFile.getUserData(Screencast.SCREENCAST_KEY)
  val audio: Screencast.Audio?
    get() = virtualFile.getUserData(Screencast.AUDIO_KEY)

  override fun getFileType() = TranscriptFileType

  override fun toString() = "Transcript file"

  override fun delete() {
    virtualFile.putUserData(Screencast.SCREENCAST_KEY, null)
    model?.let { Disposer.dispose(it) }
  }
}