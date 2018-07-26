package vladsaif.syncedit.plugin.lang.transcript.annotators

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import vladsaif.syncedit.plugin.lang.transcript.lexer.TranscriptLexerAdapter
import vladsaif.syncedit.plugin.lang.transcript.parser.TranscriptParser

class TranscriptHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return TranscriptHighlighter()
    }
}

class TranscriptHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return TranscriptLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return if (tokenType == TranscriptParser.WORD_TOKEN) {
            Highlighters.STRING_KEYS
        } else {
            Highlighters.EMPTY_KEYS
        }
    }
}

@Suppress("unused")
object Highlighters {
    val STRING = createTextAttributesKey("STRING_LITERAL", DefaultLanguageHighlighterColors.STRING)
    val TIME_OFFSET = createTextAttributesKey("TIME_OFFSET", DefaultLanguageHighlighterColors.METADATA)
    val COMMENT = DefaultLanguageHighlighterColors.LINE_COMMENT
    val BAD_CHARACTER = HighlighterColors.BAD_CHARACTER
    val HIDDEN_WORD = EditorColors.DELETED_TEXT_ATTRIBUTES

    val STRING_KEYS = arrayOf(STRING)
    val COMMENT_KEYS = arrayOf(COMMENT)
    val TIME_OFFSET_KEYS = arrayOf(TIME_OFFSET)
    val EMPTY_KEYS = arrayOf<TextAttributesKey>()
}