package vladsaif.syncedit.plugin.lang.transcript;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.transcript.lexer.TranscriptLexerAdapter;
import vladsaif.syncedit.plugin.lang.transcript.parser.TranscriptParser;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFile;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenTypes;

public class TranscriptParserDefinition implements ParserDefinition {
    public static final TokenSet STRINGS = TokenSet.create(TranscriptTokenTypes.WORD);

    public static final IFileElementType FILE = new IFileElementType(TranscriptLanguage.getInstance());

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new TranscriptLexerAdapter();
    }

    @NotNull
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    public TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @NotNull
    public PsiParser createParser(final Project project) {
        return new TranscriptParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    public PsiFile createFile(FileViewProvider viewProvider) {
        return new TranscriptFile(viewProvider);
    }

    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @NotNull
    public PsiElement createElement(ASTNode node) {
        return TranscriptTokenTypes.Factory.createElement(node);
    }
}
