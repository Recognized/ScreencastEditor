package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.ScreencastFile

/**
 * Excludes words from transcript so they are cut out from corresponding audio.
 * @see IncludeAction to revert changes
 */
class ExcludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    model.excludeWords(words.map { it.number }.toIntArray())
  }
}
