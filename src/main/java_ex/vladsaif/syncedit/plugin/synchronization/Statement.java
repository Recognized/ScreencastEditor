package vladsaif.syncedit.plugin.synchronization;

import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.editor.ClosedLongRange;

import java.util.Objects;

public class Statement implements Comparable<Statement> {
    private final TimeRange myTimeRange;
    private final int myId;
    private String myWord;

    Statement(String word, TimeRange range, int id) {
        myTimeRange = range;
        myId = id;
        myWord = word;
    }

    /**
     * Returns string associated with this statement, not including time range and id.
     *
     * @return this string.
     */
    public String getWord() {
        return myWord;
    }

    /**
     * Set string associated with this statement.
     *
     * @param word string associated with statement.
     */
    public void setWord(String word) {
        this.myWord = word;
    }

    TimeRange getTimeRange() {
        return myTimeRange;
    }

    /**
     * Returns number identifying statement in file.
     *
     * @return id of this statement.
     */
    int getId() {
        return myId;
    }

    @Override
    public String toString() {
        return "Statement{" + getWord() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statement statement = (Statement) o;
        return myId == statement.myId &&
                Objects.equals(myTimeRange, statement.myTimeRange) &&
                Objects.equals(myWord, statement.myWord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myTimeRange, myId, myWord);
    }

    @Override
    public int compareTo(@NotNull Statement o) {
        return getId() - o.getId();
    }
}
