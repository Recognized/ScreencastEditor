package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class TranscriptLexerAdapter : FlexAdapter(TranscriptLexer(null))

class TranscriptParserDefinition : ParserDefinition {
    private var elementCounter = 0

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
        elementCounter = 0
        return ManualParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return TranscriptFile(viewProvider)
    }

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        return ParserDefinition.SpaceRequirements.MAY
    }

    override fun createElement(node: ASTNode): PsiElement {
        return TranscriptWordImpl(node, elementCounter++)
    }

    companion object {
        val STRINGS = TokenSet.create(ManualParser.WORD)
        val FILE = IFileElementType(TranscriptViewLanguage)
    }
}
