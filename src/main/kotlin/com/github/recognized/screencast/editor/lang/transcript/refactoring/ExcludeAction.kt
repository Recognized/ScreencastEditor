package com.github.recognized.screencast.editor.lang.transcript.refactoring

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptWord
import com.github.recognized.screencast.editor.model.Screencast

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
