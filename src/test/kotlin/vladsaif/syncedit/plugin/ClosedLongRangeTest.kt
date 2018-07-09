package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.ClosedLongRange

import java.util.ArrayList

class ClosedLongRangeTest {

    @Test
    fun intersection_test01() {
        assertEquals(1, ClosedLongRange.intersectionLength(ClosedLongRange(40, 60), ClosedLongRange(60, 70)))
    }

    @Test
    fun intersection_test02() {
        assertEquals(0, ClosedLongRange.intersectionLength(ClosedLongRange(40, 60), ClosedLongRange(80, 90)))
    }

    @Test
    fun intersection_test03() {
        assertEquals(11, ClosedLongRange.intersectionLength(ClosedLongRange(40, 60), ClosedLongRange(50, 90)))
    }

    @Test
    fun intersection_test04() {
        assertFalse(ClosedLongRange(0, 1000).intersects(ClosedLongRange(1001, Long.MAX_VALUE)))
    }

    @Test
    fun intersection_test05() {
        assertTrue(ClosedLongRange(0, 100000).intersects(ClosedLongRange(1001, Long.MAX_VALUE)))
    }

    @Test
    fun merge_test01() {
        val ranges = ArrayList<ClosedLongRange>()
        ranges.add(ClosedLongRange(10, 30))
        ranges.add(ClosedLongRange(31, 40))
        ranges.add(ClosedLongRange(41, 50))
        val result = ClosedLongRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals(ClosedLongRange(10, 50), result.get(0))
    }

    @Test
    fun merge_test02() {
        val ranges = ArrayList<ClosedLongRange>()
        ranges.add(ClosedLongRange(100, 300))
        ranges.add(ClosedLongRange(200, 400))
        ranges.add(ClosedLongRange(250, 500))
        val result = ClosedLongRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals(ClosedLongRange(100, 500), result.get(0))
    }

    @Test
    fun merge_test03() {
        val ranges = ArrayList<ClosedLongRange>()
        ranges.add(ClosedLongRange(10, 30))
        ranges.add(ClosedLongRange(20, 40))
        ranges.add(ClosedLongRange(50, 60))
        ranges.add(ClosedLongRange(55, 65))
        ranges.add(ClosedLongRange(66, 70))
        ranges.add(ClosedLongRange(80, 90))
        val result = ClosedLongRange.mergeAdjacent(ranges).toTypedArray()
        assertEquals(3, result.size.toLong())
        val expected = arrayOf(ClosedLongRange(10, 40), ClosedLongRange(50, 70), ClosedLongRange(80, 90))
        assertArrayEquals(expected, result)
    }
}
