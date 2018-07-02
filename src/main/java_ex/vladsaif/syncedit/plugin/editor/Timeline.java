package vladsaif.syncedit.plugin.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Timeline {
    private static final Comparator<TimeRange> INTERSECTS_CMP = (a, b) -> {
        if (a.intersects(b)) return 0;
        return a.getStartOffsetMs() - b.getStartOffsetMs();
    };

    /**
     * Invariant: sorted in ascending order, distance between each other at least one
     */
    private final List<TimeRange> deletedRanges = new ArrayList<>();
    private TimeRange lastCalculated;
    private int cachedIndex;
    private int cachedAccum;

    public List<TimeRange> getDeletedRanges() {
        return Collections.unmodifiableList(deletedRanges);
    }

    public void reset() {
        deletedRanges.clear();
        lastCalculated = null;
    }

    public void load(Timeline other) {
        reset();
        deletedRanges.addAll(other.deletedRanges);
    }

    public Timeline copy() {
        Timeline timeline = new Timeline();
        timeline.load(this);
        return timeline;
    }

    private static int toInsertPosition(int x) {
        return x < 0 ? -x - 1 : x;
    }

    public void add(TimeRange range) {
        lastCalculated = null;
        int pos_start = Collections.binarySearch(deletedRanges, TimeRange.from(range.getStartOffsetMs(), 1), INTERSECTS_CMP);
        int pos_end = Collections.binarySearch(deletedRanges, TimeRange.from(range.getEndOffsetMs(), 1), INTERSECTS_CMP);
        int lastTouched = pos_end < 0 ? toInsertPosition(pos_end) - 1 : pos_end;
        List<TimeRange> toEdit = deletedRanges.subList(toInsertPosition(pos_start), lastTouched + 1);
        if (!toEdit.isEmpty()) {
            int oldStart = toEdit.get(0).getStartOffsetMs();
            int oldEnd = toEdit.get(toEdit.size() - 1).getEndOffsetMs();
            // Clear all touched
            toEdit.clear();
            // Add piece of start if it was replaced
            if (oldStart < range.getStartOffsetMs()) {
                toEdit.add(new TimeRange(oldStart, range.getStartOffsetMs() - 1));
            }
            if (oldEnd > range.getEndOffsetMs()) {
                toEdit.add(new TimeRange(range.getEndOffsetMs() + 1, oldEnd));
            }
        }
    }

    public void delete(TimeRange range) {
        lastCalculated = null;
        deletedRanges.add(range);
        List<TimeRange> merged = TimeRange.mergeAdjacent(deletedRanges);
        deletedRanges.clear();
        deletedRanges.addAll(merged);
    }

    public TimeRange impose(TimeRange range) {
        int accum = 0;
        int i = 0;
        if (lastCalculated != null && lastCalculated.getStartOffsetMs() <= range.getStartOffsetMs()) {
            i = cachedIndex;
            accum = cachedAccum;
        }
        while (i < deletedRanges.size() && deletedRanges.get(i).getEndOffsetMs() < range.getStartOffsetMs()) {
            accum += deletedRanges.get(i).getLength();
            ++i;
        }
        cachedIndex = i;
        cachedAccum = accum;
        lastCalculated = range;
        int left = range.getStartOffsetMs() - accum;
        int right = range.getEndOffsetMs() - accum;
        TimeRange leftPart = new TimeRange(0, Math.max(range.getStartOffsetMs() - 1, 0));
        TimeRange rightPart = new TimeRange(0, range.getEndOffsetMs());
        for (; i < deletedRanges.size(); ++i) {
            TimeRange cur = deletedRanges.get(i);
            if (cur.getStartOffsetMs() <= range.getEndOffsetMs()) {
                right -= TimeRange.intersectionLength(rightPart, cur);
                left -= TimeRange.intersectionLength(leftPart, cur);
            } else break;
        }
        return new TimeRange(left, right);
    }

    @Override
    public String toString() {
        return "Deletions: " + deletedRanges.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}
