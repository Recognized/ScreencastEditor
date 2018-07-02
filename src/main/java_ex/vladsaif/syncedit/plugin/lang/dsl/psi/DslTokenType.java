package vladsaif.syncedit.plugin.lang.dsl.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.dsl.Dsl;

public class DslTokenType extends IElementType {
    public static final DslTokenType TOO_BIG = new DslTokenType("TOO_BIG");

    public DslTokenType(@NotNull String debugName) {
        super(debugName, Dsl.getInstance());
    }

    @Override
    public String toString() {
        return "DslTokenType." + super.toString();
    }
}
