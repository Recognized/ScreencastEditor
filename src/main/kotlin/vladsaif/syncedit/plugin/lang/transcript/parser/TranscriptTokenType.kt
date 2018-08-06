package vladsaif.syncedit.plugin.lang.transcript.parser

import com.intellij.psi.tree.IElementType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLanguage

class TranscriptTokenType(debugName: String) : IElementType(debugName, TranscriptLanguage) {
  override fun toString(): String {
    return "TranscriptTokenType." + super.toString()
  }
}
