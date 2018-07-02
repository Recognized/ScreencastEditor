package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.util.TextRange;

public class Edition {
    private final TextRange myRange;
    private final EditionType myType;
    private final String myText;
    private final int myInsertPosition;

    private Edition(EditionType type, TextRange range, String text, int insertPosition) {
        myRange = range;
        myType = type;
        myText = text;
        myInsertPosition = insertPosition;
    }

    static Edition makeDeletion(TextRange range) {
        return new Edition(EditionType.DELETE, range, null, 0);
    }

    static Edition makeReplacement(TextRange range, String newText) {
        return new Edition(EditionType.REPLACE, range, newText, 0);
    }

    static Edition makeInsertion(int position, String what) {
        return new Edition(EditionType.INSERT, null, what, position);
    }

    private void errorWrongType() {
        throw new AssertionError("Wrong request, edition type is: " + myType);
    }

    public int getInsertPosition() {
        if (!myType.equals(EditionType.INSERT)) errorWrongType();
        return myInsertPosition;
    }

    public TextRange getRange() {
        if (myType.equals(EditionType.INSERT)) errorWrongType();
        return myRange;
    }

    public String getText() {
        if (myType.equals(EditionType.DELETE)) errorWrongType();
        return myText;
    }

    boolean isDeletion() {
        return myType.equals(EditionType.DELETE);
    }

    boolean isInsertion() {
        return myType.equals(EditionType.INSERT);
    }

    boolean isReplacement() {
        return myType.equals(EditionType.REPLACE);
    }

    private enum EditionType {
        DELETE, INSERT, REPLACE
    }

    @Override
    public String toString() {
        String ans = "" + myType;
        if (isDeletion()) {
            ans += myRange;
        } else if (isInsertion()) {
            ans += myInsertPosition;
        } else {
            ans += myRange + myText.substring(0, Math.min(10, myText.length()));
        }
        return ans;
    }
}