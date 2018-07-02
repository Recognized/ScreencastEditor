package vladsaif.syncedit.plugin.lang.transcript;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class TranscriptLanguage extends Language {
    private static final TranscriptLanguage INSTANCE = new TranscriptLanguage();

    public static TranscriptLanguage getInstance() {
        return INSTANCE;
    }

    protected TranscriptLanguage() {
        super("Gui test audio transcript");
    }
}
