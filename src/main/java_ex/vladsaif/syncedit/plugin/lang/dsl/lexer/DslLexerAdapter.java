package vladsaif.syncedit.plugin.lang.dsl.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.dsl.lexer.DslLexer;

public class DslLexerAdapter extends FlexAdapter {
    public DslLexerAdapter() {
        super(new DslLexer(null));
    }
}
