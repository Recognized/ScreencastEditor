package vladsaif.syncedit.plugin.lang.transcript;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.Highlighters;
import vladsaif.syncedit.plugin.lang.transcript.lexer.TranscriptLexerAdapter;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenType;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenTypes;

public class TranscriptHighlighter extends SyntaxHighlighterBase {
    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new TranscriptLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType == null) {
            return Highlighters.EMPTY_KEYS;
        } else if (tokenType.equals(TranscriptTokenTypes.WORD)) {
            return Highlighters.STRING_KEYS;
        } else if (tokenType.equals(TranscriptTokenTypes.INTEGER_LITERAL)) {
            return Highlighters.TIME_OFFSET_KEYS;
        } else {
            return Highlighters.EMPTY_KEYS;
        }
    }
}
