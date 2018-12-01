package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.Screencast

/**
 * Excludes words from transcript so they are cut out from corresponding audio.
 * @see IncludeAction to revert changes
 */
class ExcludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: Screencast, audio: Screencast.Audio, words: List<TranscriptWord>) {
    model.performModification {
      getEditable(audio).excludeWords(words.map { it.number }.toIntArray())
    }
  }
}
