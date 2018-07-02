package vladsaif.syncedit.plugin.lang.transcript.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptFileType;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptLanguage;

public class TranscriptFile extends PsiFileBase {
    public TranscriptFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, TranscriptLanguage.getInstance());
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return TranscriptFileType.getInstance();
    }

    @Override
    public String toString() {
        return "Audio transcript myPsiFile";
    }
}
