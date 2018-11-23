package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.ScreencastFile

/**
 * Includes previously excluded or muted words back to transcript.
 * @see ExcludeAction
 */
class IncludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    model.performModification {
      showWords(words.map { it.number }.toIntArray())
    }
  }
}
