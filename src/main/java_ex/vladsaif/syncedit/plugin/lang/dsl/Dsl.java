package vladsaif.syncedit.plugin.lang.dsl;

import com.intellij.lang.Language;

public class Dsl extends Language {
    private static final Dsl INSTANCE = new Dsl();

    private Dsl() {
        super("GUI test script");
    }

    public static Dsl getInstance() {
        return INSTANCE;
    }
}
