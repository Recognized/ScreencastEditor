package com.github.recognized.screencast.editor.lang.transcript.parser

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptLanguage
import com.intellij.psi.tree.IElementType

class TranscriptTokenType(debugName: String) : IElementType(debugName, TranscriptLanguage) {
  override fun toString(): String {
    return "TranscriptTokenType." + super.toString()
  }
}
