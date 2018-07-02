package vladsaif.syncedit.plugin.lang.dsl;

import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.icons.AllIcons;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslTokenTypes;

import javax.swing.*;

public class DslFileType extends LanguageFileType {
    private static final DslFileType INSTANCE = new DslFileType();

    private DslFileType() {
        super(Dsl.getInstance());
        PairedBraceMatcher matcher = new PairedBraceMatcher() {
            private final BracePair[] BRACES = new BracePair[]{
                    new BracePair(DslTokenTypes.OPEN_BRACE, DslTokenTypes.CLOSE_BRACE, true)
            };
            @NotNull
            @Override
            public BracePair[] getPairs() {
                return BRACES;
            }

            @Override
            public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
                return true;
            }

            @Override
            public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
                PsiElement brace = file.findElementAt(openingBraceOffset);
                if (brace == null) {
                    return openingBraceOffset;
                }
                PsiElement statement = brace.getPrevSibling();
                if (statement == null) {
                    return openingBraceOffset;
                }
                return statement.getTextOffset();
            }
        };
        BraceMatchingUtil.registerBraceMatcher(this, new PairedBraceMatcherAdapter(matcher, Dsl.getInstance()));
    }

    @NotNull
    @Override
    public String getName() {
        return "GUI test script";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Gui test script myPsiFile";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "guitest";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.General.Run;
    }

    public static DslFileType getInstance() {
        return INSTANCE;
    }
}
