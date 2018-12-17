package com.github.recognized.screencast.editor.lang.transcript.refactoring

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptWord
import com.github.recognized.screencast.editor.model.Screencast

class MuteAction : TranscriptRefactoringAction() {

  override fun doAction(model: Screencast, audio: Screencast.Audio, words: List<TranscriptWord>) {
    model.performModification { getEditable(audio).muteWords(words.map { it.number }.toIntArray()) }
  }
}
