package vladsaif.syncedit.plugin.editor;

import java.util.*;

/**
 * Represents time range with included ends
 */
public class TimeRange implements Comparable<TimeRange> {
    private final int startOffsetMs;
    private final int endOffsetMs;

    public TimeRange(int startOffsetMs, int endOffsetMs) {
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
    }

    public static TimeRange from(int startOffsetMs, int length) {
        return new TimeRange(startOffsetMs, startOffsetMs + length - 1);
    }

    public static int intersectionLength(TimeRange a, TimeRange b) {
        return Math.max(Math.min(a.endOffsetMs, b.endOffsetMs) - Math.max(a.startOffsetMs, b.startOffsetMs) + 1, 0);
    }

    public boolean contains(TimeRange other) {
        return startOffsetMs <= other.startOffsetMs && other.endOffsetMs <= endOffsetMs;
    }

    public int getStartOffsetMs() {
        return startOffsetMs;
    }

    public int getEndOffsetMs() {
        return endOffsetMs;
    }

    public TimeRange shift(int delta) {
        return new TimeRange(startOffsetMs + delta, endOffsetMs + delta);
    }

    public TimeRange stretchRight(int delta) {
        return new TimeRange(startOffsetMs, endOffsetMs + delta);
    }

    public int getLength() {
        return endOffsetMs - startOffsetMs + 1;
    }

    public boolean isValid() {
        return startOffsetMs <= endOffsetMs;
    }

    public boolean intersects(TimeRange other) {
        return Math.max(endOffsetMs, other.endOffsetMs) - Math.min(startOffsetMs, other.startOffsetMs) + 1 < getLength() + other.getLength();
    }

    /**
     * Merges adjacent ranges into one.
     * Two ranges are considered adjacent if <code>one.getEndOffset() + 1 >= another.getStartOffset()</code>.
     *
     * @param ranges the ranges to merge
     * @return new set with merged ranges
     */
    public static Set<TimeRange> mergeAdjacent(Set<TimeRange> ranges) {
        return new HashSet<>(mergeAdjacent(new ArrayList<>(ranges)));
    }

    /**
     * Merges adjacent ranges into one.
     * Two ranges are considered adjacent if <code>one.getEndOffset() + 1 >= another.getStartOffset()</code>.
     *
     * @param list ranges to merge
     * @return new list with merged ranges
     */
    public static List<TimeRange> mergeAdjacent(List<TimeRange> list) {
        Collections.sort(list);
        List<TimeRange> result = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < list.size(); ++i) {
            if (i == 0 || list.get(i).getStartOffsetMs() > list.get(i - 1).getEndOffsetMs() + 1) {
                if (start != -1) {
                    result.add(new TimeRange(start, list.get(i - 1).getEndOffsetMs()));
                }
                start = list.get(i).startOffsetMs;
            }
        }
        if (start != -1) {
            result.add(new TimeRange(start, list.get(list.size() - 1).getEndOffsetMs()));
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeRange timeRange = (TimeRange) o;
        return startOffsetMs == timeRange.startOffsetMs &&
                endOffsetMs == timeRange.endOffsetMs;
    }

    @Override
    public int compareTo(TimeRange other) {
        return startOffsetMs - other.startOffsetMs;
    }

    @Override
    public int hashCode() {
        return startOffsetMs + endOffsetMs;
    }

    @Override
    public String toString() {
        return "[" + startOffsetMs + "ms, " + endOffsetMs + "ms]";
    }
}
