package vladsaif.syncedit.plugin.lang.dsl.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.lang.dsl.Dsl;

public class DslElementType extends IElementType {
    public static final DslElementType COMMENT = new DslElementType("COMMENT");

    public DslElementType(@NotNull String debugName) {
        super(debugName, Dsl.getInstance());
    }
}
