package vladsaif.syncedit.plugin.lang.transcript.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import vladsaif.syncedit.plugin.lang.transcript.parser.TranscriptParser;

%%

%class TranscriptLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE_CHAR=[\u00A0\n\t\f]
WORD_CHAR = [^\u00A0\n\t\f]

%%

<YYINITIAL> {
    {WHITE_SPACE_CHAR}+             { return TokenType.WHITE_SPACE; }
    {WORD_CHAR}+                    { return TranscriptParser.Companion.getWORD_TOKEN(); }
}