package vladsaif.syncedit.plugin.lang.dsl.lexer;

import vladsaif.syncedit.plugin.lang.dsl.psi.*;
import vladsaif.syncedit.plugin.lang.dsl.parser.*;
import vladsaif.syncedit.plugin.lang.dsl.*;
import com.intellij.psi.TokenType;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;

%%

%class DslLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

DIGIT=[0-9]
HEX_DIGIT=[0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \t\f]
CRLF=\n|(\r\n)

PLAIN_IDENTIFIER=[:jletter:] [:jletterdigit:]*
ESCAPED_IDENTIFIER = `{PLAIN_IDENTIFIER}`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
DECLARATION_IDENTIFIER = "&" {PLAIN_IDENTIFIER}
FIELD_IDENTIFIER = \${IDENTIFIER}
LABEL_IDENTIFIER = \@{IDENTIFIER}

BLOCK_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT})*
LONG_LITERAL=({INTEGER_LITERAL})[Ll]
RANGE_INTEGER_LITERAL=(0|([1-9](([0-9]){0,8})))
TOO_BIG_INTEGER_LITERAL=([1-9]([0-9]{9})[0-9]*)

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGIT})+"."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGIT})+({EXPONENT_PART})
FLOATING_POINT_LITERAL4=({DIGIT})+
EXPONENT_PART=[Ee]["+""-"]?({DIGIT})*
HEX_FLOAT_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Ff]
//HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Dd]?
HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}?
BINARY_EXPONENT=[Pp][+-]?{DIGIT}+
HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+
//HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}\.|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: introduce symbols (e.g. 'foo) as another way to write string literals
STRING_LITERAL=\"([^\\\"\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ANGLE_STRING_LITERAL=\<([^\\\>\n])*(\>|\\)?
ESCAPE_SEQUENCE=\\[^\n]

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
ONE_TWO_QUO = (\"[^\"]) | (\"\"[^\"])
QUO_STRING_CHAR = [^\"] | {ONE_TWO_QUO}
RAW_STRING_LITERAL = {THREE_QUO} {QUO_STRING_CHAR}* {THREE_QUO}?

%state META_COMMENT

%%

<YYINITIAL> {
    {BLOCK_COMMENT}                 { return DslElementType.COMMENT; }
    {DOC_COMMENT}                   { return DslElementType.COMMENT; }
    "/""/"                          { yybegin(META_COMMENT); return DslTokenTypes.EOL_COMMENT_START; }
    ({WHITE_SPACE_CHAR})+           { return TokenType.WHITE_SPACE; }
    {STRING_LITERAL}                { return DslTokenTypes.STRING; }
    {ANGLE_STRING_LITERAL}          { return DslTokenTypes.STRING; }
    {IDENTIFIER}                    { return DslTokenTypes.CHAR; }
    "[" {IDENTIFIER} "]"            { return DslTokenTypes.CHAR; }
    {DECLARATION_IDENTIFIER}        { return DslTokenTypes.CHAR; }
    {CRLF}                          { return DslTokenTypes.CRLF; }
    "{"                             { return DslTokenTypes.OPEN_BRACE; }
    "}"                             { return DslTokenTypes.CLOSE_BRACE; }
    .                               { return DslTokenTypes.CHAR; }
}

<META_COMMENT> {
    {TOO_BIG_INTEGER_LITERAL}       { return DslTokenType.TOO_BIG; }
    {RANGE_INTEGER_LITERAL}         { return DslTokenTypes.INTEGER_LITERAL; }
    {CRLF}                          { yybegin(YYINITIAL); return DslTokenTypes.CRLF; }
    ({WHITE_SPACE_CHAR})+           { return TokenType.WHITE_SPACE; }
    ","                             { return DslTokenTypes.COMMA; }
    "["                             { return DslTokenTypes.OPEN_BRACKET; }
    "]"                             { return DslTokenTypes.CLOSE_BRACKET; }
    .                               { return DslTokenTypes.CHAR; }
}