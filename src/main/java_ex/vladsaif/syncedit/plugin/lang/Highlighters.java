package vladsaif.syncedit.plugin.lang;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class Highlighters {
    public static final TextAttributesKey STRING =
            createTextAttributesKey("STRING_LITERAL", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey TIME_OFFSET =
            createTextAttributesKey("TIME_OFFSET", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("SIMPLE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("SIMPLE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    public static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    public static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    public static final TextAttributesKey[] TIME_OFFSET_KEYS = new TextAttributesKey[]{TIME_OFFSET};
    public static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
}
