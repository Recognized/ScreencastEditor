package vladsaif.syncedit.plugin.lang.transcript.lexer;

import vladsaif.syncedit.plugin.lang.transcript.psi.*;
import vladsaif.syncedit.plugin.lang.transcript.parser.*;
import vladsaif.syncedit.plugin.lang.transcript.*;
import com.intellij.psi.TokenType;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class TranscriptLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE_CHAR=[\ \n\t\f]
DECIMAL_INTEGER_LITERAL=(0|([1-9](([0-9]){0,8})))
TOO_BIG_INTEGER_LITERAL=([1-9]([0-9]{9})[0-9]*)
COMMA = \,
PUNCTUATION = [\,:\.?!;\"\"\']
WORD = [^\ \n\t\f\[\]\,:\.?!;\"\"\']+

%state IN_BRACKETS

%%

<YYINITIAL> {
    {WHITE_SPACE_CHAR}+             { return TokenType.WHITE_SPACE; }
    {PUNCTUATION}+                  { return TokenType.WHITE_SPACE; }
    \[                              { yybegin(IN_BRACKETS); return TranscriptTokenTypes.OPEN_BRACKET; }
    {WORD}+                         { return TranscriptTokenTypes.WORD; }
}

<IN_BRACKETS> {
    {WHITE_SPACE_CHAR}+             { return TokenType.WHITE_SPACE; }
    \]                              { yybegin(YYINITIAL); return TranscriptTokenTypes.CLOSE_BRACKET; }
    {COMMA}                         { return TranscriptTokenTypes.COMMA; }
    {DECIMAL_INTEGER_LITERAL}       { return TranscriptTokenTypes.INTEGER_LITERAL; }
    {TOO_BIG_INTEGER_LITERAL}       { return TranscriptTokenType.TOO_BIG; }
}