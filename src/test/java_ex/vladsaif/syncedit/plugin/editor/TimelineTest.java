package vladsaif.syncedit.plugin.editor;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TimelineTest {
    private final Timeline model = new Timeline();

    @Before
    public void setUpModel() {
        model.reset();
        for (int i = 0; i < 10; ++i) {
            model.delete(TimeRange.from(i * 20, 10));
        }
    }

    @Test
    public void deleting_ranges_test01() {
        for (int i = 0; i < 10; ++i) {
            model.add(TimeRange.from(i * 20, 10));
        }
        assertEquals(0, model.getDeletedRanges().size());
    }

    @Test
    public void deleting_ranges_test02() {
        for (int i = 10; i >= 0; --i) {
            model.add(TimeRange.from(i * 20 + 10, 10));
        }
        assertEquals(10, model.getDeletedRanges().size());
    }

    @Test
    public void deleting_ranges_test03() {
        model.add(new TimeRange(25, 89));
        assertEquals(7, model.getDeletedRanges().size());
    }

    @Test
    public void deleting_ranges_test04() {
        model.add(new TimeRange(10, 59));
        assertEquals(8, model.getDeletedRanges().size());
    }

    @Test
    public void deleting_ranges_test05() {
        model.reset();
        model.delete(TimeRange.from(1, 30));
        assertEquals(1, model.getDeletedRanges().size());
        model.delete(TimeRange.from(0, 50));
        assertEquals(1, model.getDeletedRanges().size());
        model.delete(TimeRange.from(5, 60));
        assertEquals(1, model.getDeletedRanges().size());
        model.delete(TimeRange.from(40, 10));
        assertEquals(1, model.getDeletedRanges().size());
    }

    @Test
    public void adding_ranges_test01() {
        model.reset();
        model.delete(TimeRange.from(1, 30));
        model.add(TimeRange.from(5, 5));
        model.add(TimeRange.from(12, 5));
        assertEquals(3, model.getDeletedRanges().size());
    }

    @Test
    public void adding_ranges_test02() {
        model.reset();
        model.delete(TimeRange.from(1, 100));
        model.add(TimeRange.from(5, 5));
        model.add(TimeRange.from(12, 5));
        model.add(TimeRange.from(0, 3));
        model.add(TimeRange.from(90, 20));
        assertEquals(3, model.getDeletedRanges().size());
    }

    @Test
    public void impose_test01() {
        model.reset();
        model.delete(new TimeRange(40, 60));
        assertEquals(new TimeRange(30, 39), model.impose(new TimeRange(30, 40)));
    }

    @Test
    public void impose_test02() {
        assertEquals(new TimeRange(20, 45), model.impose(new TimeRange(45, 95)));
    }

    @Test
    public void impose_test03() {
        model.reset();
        model.delete(new TimeRange(10, 100));
        assertFalse(model.impose(new TimeRange(50, 60)).isValid());
    }

    @Test
    public void impose_test04() {
        assertEquals(model.impose(new TimeRange(40, 60)), model.impose(new TimeRange(40, 60)));
    }

    @Test
    public void impose_test05() {
        model.reset();
        Random gen = new Random(System.currentTimeMillis());
        for (int i = 0; i < 100; ++i) {
            TimeRange rand = TimeRange.from(gen.nextInt() % 300, gen.nextInt() % 300);
            assertEquals(rand, model.impose(rand));
        }
    }

    @Test
    public void impose_cache_test01() {
        Random gen = new Random(System.currentTimeMillis());
        List<TimeRange> ranges = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            ranges.add(TimeRange.from(gen.nextInt() % 300, gen.nextInt() % 300));
        }
        List<TimeRange> resultsWithReset = new ArrayList<>();
        List<TimeRange> resultsNoReset = new ArrayList<>();
        for (TimeRange x : ranges) {
            setUpModel();
            resultsWithReset.add(model.impose(x));
        }
        for (TimeRange x : ranges) {
            resultsNoReset.add(model.impose(x));
        }
        assertArrayEquals(resultsNoReset.toArray(), resultsWithReset.toArray());
    }


}
