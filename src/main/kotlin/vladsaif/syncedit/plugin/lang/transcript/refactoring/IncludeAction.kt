package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

/**
 * Includes previously excluded or muted words back to transcript.
 * @see ExcludeAction
 */
class IncludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    model.showWords(words.map { it.number }.toIntArray())
  }
}
