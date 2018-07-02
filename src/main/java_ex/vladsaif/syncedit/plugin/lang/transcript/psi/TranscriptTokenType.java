package vladsaif.syncedit.plugin.lang.transcript.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptLanguage;

public class TranscriptTokenType extends IElementType {
    public static final IElementType TOO_BIG = new TranscriptTokenType("TOO_BIG_LITERAL");

    public TranscriptTokenType(@NotNull String debugName) {
        super(debugName, TranscriptLanguage.getInstance());
    }

    @Override
    public String toString() {
        return "TranscriptTokenType." + super.toString();
    }
}
