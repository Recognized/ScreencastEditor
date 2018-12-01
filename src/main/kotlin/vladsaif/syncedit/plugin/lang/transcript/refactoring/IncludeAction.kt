package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.Screencast

/**
 * Includes previously excluded or muted words back to transcript.
 * @see ExcludeAction
 */
class IncludeAction : TranscriptRefactoringAction() {

  override fun doAction(model: Screencast, audio: Screencast.Audio, words: List<TranscriptWord>) {
    model.performModification {
      getEditable(audio).showWords(words.map { it.number }.toIntArray())
    }
  }
}
