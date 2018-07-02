package vladsaif.syncedit.plugin.lang.transcript.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;
import org.jetbrains.annotations.NotNull;

public class TranscriptLexerAdapter extends FlexAdapter {
    public TranscriptLexerAdapter() {
        super(new TranscriptLexer(null));
    }
}
