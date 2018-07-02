package vladsaif.syncedit.plugin.lang.transcript.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptLanguage;

public class TranscriptElementType extends IElementType {
    public TranscriptElementType(@NotNull String debugName) {
        super(debugName, TranscriptLanguage.getInstance());
    }
}
