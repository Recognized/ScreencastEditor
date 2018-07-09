package vladsaif.syncedit.plugin.synchronization;

import vladsaif.syncedit.plugin.editor.ClosedLongRange;

import java.util.Objects;

public class EnclosingStatement extends Statement {
    private boolean enclosingOtherStatements;
    private int depth;

    EnclosingStatement(String word, TimeRange range, int id, int depth, boolean enclosingOtherStatements) {
        super(word, range, id);
        this.enclosingOtherStatements = enclosingOtherStatements;
        this.depth = depth;
    }

    boolean isWide() {
        return enclosingOtherStatements;
    }

    @Override
    public String toString() {
        return "EnclosingStatement{" + getWord() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EnclosingStatement that = (EnclosingStatement) o;
        return enclosingOtherStatements == that.enclosingOtherStatements;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), enclosingOtherStatements);
    }

    public int getDepth() {
        return depth;
    }
}
