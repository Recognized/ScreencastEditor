package vladsaif.syncedit.plugin.editor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TimeRangeTest {

    @Test
    public void intersection_test01() {
        assertEquals(1, TimeRange.intersectionLength(new TimeRange(40, 60), new TimeRange(60, 70)));
    }

    @Test
    public void intersection_test02() {
        assertEquals(0, TimeRange.intersectionLength(new TimeRange(40, 60), new TimeRange(80, 90)));
    }

    @Test
    public void intersection_test03() {
        assertEquals(11, TimeRange.intersectionLength(new TimeRange(40, 60), new TimeRange(50, 90)));
    }

    @Test
    public void merge_test01() {
        List<TimeRange> ranges = new ArrayList<>();
        ranges.add(new TimeRange(10, 30));
        ranges.add(new TimeRange(31, 40));
        ranges.add(new TimeRange(41, 50));
        List<TimeRange> result = TimeRange.mergeAdjacent(ranges);
        assertEquals(1, result.size());
        assertEquals(new TimeRange(10, 50), result.get(0));
    }

    @Test
    public void merge_test02() {
        List<TimeRange> ranges = new ArrayList<>();
        ranges.add(new TimeRange(100, 300));
        ranges.add(new TimeRange(200, 400));
        ranges.add(new TimeRange(250, 500));
        List<TimeRange> result = TimeRange.mergeAdjacent(ranges);
        assertEquals(1, result.size());
        assertEquals(new TimeRange(100, 500), result.get(0));
    }

    @Test
    public void merge_test03() {
        List<TimeRange> ranges = new ArrayList<>();
        ranges.add(new TimeRange(10, 30));
        ranges.add(new TimeRange(20, 40));
        ranges.add(new TimeRange(50, 60));
        ranges.add(new TimeRange(55, 65));
        ranges.add(new TimeRange(66, 70));
        ranges.add(new TimeRange(80, 90));
        TimeRange[] result = TimeRange.mergeAdjacent(ranges).toArray(new TimeRange[0]);
        assertEquals(3, result.length);
        TimeRange[] expected = new TimeRange[]{new TimeRange(10, 40), new TimeRange(50, 70), new TimeRange(80, 90)};
        assertArrayEquals(expected, result);
    }
}
