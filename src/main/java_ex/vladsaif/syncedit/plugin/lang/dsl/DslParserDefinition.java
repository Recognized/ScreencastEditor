package vladsaif.syncedit.plugin.lang.dsl;

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
import vladsaif.syncedit.plugin.lang.dsl.lexer.DslLexerAdapter;
import vladsaif.syncedit.plugin.lang.dsl.parser.DslParser;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslElementType;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslFile;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslTokenTypes;

public class DslParserDefinition implements ParserDefinition {
    public static final TokenSet COMMENTS = TokenSet.create(DslElementType.COMMENT);
    public static final TokenSet STRINGS = TokenSet.create(DslTokenTypes.STRING);

    public static final IFileElementType FILE = new IFileElementType(Dsl.getInstance());

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new DslLexerAdapter();
    }

    @NotNull
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    public TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @NotNull
    public PsiParser createParser(final Project project) {
        return new DslParser();
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    public PsiFile createFile(FileViewProvider viewProvider) {
        return new DslFile(viewProvider);
    }

    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @NotNull
    public PsiElement createElement(ASTNode node) {
        return DslTokenTypes.Factory.createElement(node);
    }
}
