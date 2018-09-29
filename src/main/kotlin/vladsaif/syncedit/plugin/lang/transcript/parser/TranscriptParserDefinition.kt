package vladsaif.syncedit.plugin.lang.transcript.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import vladsaif.syncedit.plugin.lang.transcript.lexer.TranscriptLexerAdapter
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLanguage
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWordImpl


class TranscriptParserDefinition : ParserDefinition {

  override fun createLexer(project: Project): Lexer {
    return TranscriptLexerAdapter()
  }

  override fun getCommentTokens(): TokenSet {
    return TokenSet.EMPTY
  }

  override fun getStringLiteralElements(): TokenSet {
    return STRINGS
  }

  override fun createParser(project: Project): PsiParser {
    return TranscriptParser()
  }

  override fun getFileNodeType(): IFileElementType {
    return FILE
  }

  override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return TranscriptPsiFile(viewProvider)
  }

  override fun createElement(node: ASTNode): PsiElement {
    return TranscriptWordImpl(node)
  }

  companion object {
    val STRINGS = TokenSet.create(TranscriptParser.WORD)
    val FILE = IFileElementType(TranscriptLanguage)
  }
}
