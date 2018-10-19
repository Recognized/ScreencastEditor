package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.ScreencastFile

class MuteAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    model.muteWords(words.map { it.number }.toIntArray())
  }
}
