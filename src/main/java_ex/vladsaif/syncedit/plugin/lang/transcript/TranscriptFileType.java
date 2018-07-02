package vladsaif.syncedit.plugin.lang.transcript;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TranscriptFileType extends LanguageFileType {
    private static final TranscriptFileType INSTANCE = new TranscriptFileType();

    public static TranscriptFileType getInstance() {
        return INSTANCE;
    }

    private TranscriptFileType() {
        super(TranscriptLanguage.getInstance());
    }

    @NotNull
    @Override
    public String getName() {
        return "Audio transcript";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Audio transcript myPsiFile";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "transcript";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
