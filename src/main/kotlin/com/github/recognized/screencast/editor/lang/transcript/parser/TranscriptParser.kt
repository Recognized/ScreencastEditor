package com.github.recognized.screencast.editor.lang.transcript.parser

import com.github.recognized.screencast.editor.lang.transcript.lexer.TranscriptElementType
import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

class TranscriptParser : PsiParser, LightPsiParser {
  override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
    parseLight(root, builder)
    return builder.treeBuilt
  }

  override fun parseLight(root: IElementType?, builder: PsiBuilder?) {
    root ?: return
    builder ?: return
    val start = builder.mark()
    while (builder.tokenType != null) {
      val mark = builder.mark()
      builder.advanceLexer()
      mark.done(WORD)
    }
    start.done(root)
  }

  companion object {
    val WORD: IElementType = TranscriptElementType("WORD")
    val WORD_TOKEN: IElementType = TranscriptTokenType("WORD_TOKEN")
  }
}