package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

/**
 * Excludes words from transcript so they are cut out from corresponding audio.
 * @see IncludeAction to revert changes
 */
class ExcludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: MultimediaModel, words: List<TranscriptWord>) {
    model.excludeWords(words.map { it.number }.toIntArray())
  }
}
