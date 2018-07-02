package vladsaif.syncedit.plugin.lang.dsl;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.Highlighters;
import vladsaif.syncedit.plugin.lang.dsl.lexer.DslLexerAdapter;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslElementType;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslTokenTypes;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class DslSyntaxHighlighter extends SyntaxHighlighterBase {


    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new DslLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType == null) {
            return Highlighters.EMPTY_KEYS;
        } else if (tokenType.equals(DslTokenTypes.STRING)) {
            return Highlighters.STRING_KEYS;
        } else if (tokenType.equals(DslElementType.COMMENT)) {
            return Highlighters.COMMENT_KEYS;
        } else if (tokenType.equals(DslTokenTypes.INTEGER_LITERAL)) {
            return Highlighters.TIME_OFFSET_KEYS;
        } else {
            return Highlighters.EMPTY_KEYS;
        }
    }
}