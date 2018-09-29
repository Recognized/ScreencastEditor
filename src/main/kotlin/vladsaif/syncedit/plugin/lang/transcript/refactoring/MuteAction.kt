package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class MuteAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    model.muteWords(words.map { it.number }.toIntArray())
  }
}
