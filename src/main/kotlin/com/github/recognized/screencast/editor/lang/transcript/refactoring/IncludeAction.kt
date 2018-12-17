package com.github.recognized.screencast.editor.lang.transcript.refactoring

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptWord
import com.github.recognized.screencast.editor.model.Screencast

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
