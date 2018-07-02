package vladsaif.syncedit.plugin.lang.dsl.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.dsl.Dsl;
import vladsaif.syncedit.plugin.lang.dsl.DslFileType;

public class DslFile extends PsiFileBase {
    public DslFile(FileViewProvider provider) {
        super(provider, Dsl.getInstance());
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return DslFileType.getInstance();
    }

    @Override
    public String toString() {
        return "GUI test script myPsiFile";
    }
}
