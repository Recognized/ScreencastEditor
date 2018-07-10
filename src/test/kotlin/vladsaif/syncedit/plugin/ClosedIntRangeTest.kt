package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test

import java.util.ArrayList

class ClosedIntRangeTest {

    @Test
    fun intersection_test01() {
        assertEquals(1, (ClosedIntRange(40, 60) intersect ClosedIntRange(60, 70)).length)
    }

    @Test
    fun intersection_test02() {
        assertEquals(0, (ClosedIntRange(40, 60) intersect ClosedIntRange(80, 90)).length)
    }

    @Test
    fun intersection_test03() {
        assertEquals(11, (ClosedIntRange(40, 60) intersect ClosedIntRange(50, 90)).length)
    }

    @Test
    fun intersection_test04() {
        assertFalse(ClosedIntRange(0, 1000).intersects(ClosedIntRange(1001, Int.MAX_VALUE)))
    }

    @Test
    fun intersection_test05() {
        assertTrue(ClosedIntRange(0, 100000).intersects(ClosedIntRange(1001, Int.MAX_VALUE)))
    }

    @Test
    fun merge_test01() {
        val ranges = ArrayList<ClosedIntRange>()
        ranges.add(ClosedIntRange(10, 30))
        ranges.add(ClosedIntRange(31, 40))
        ranges.add(ClosedIntRange(41, 50))
        val result = ClosedIntRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals(ClosedIntRange(10, 50), result.get(0))
    }

    @Test
    fun merge_test02() {
        val ranges = ArrayList<ClosedIntRange>()
        ranges.add(ClosedIntRange(100, 300))
        ranges.add(ClosedIntRange(200, 400))
        ranges.add(ClosedIntRange(250, 500))
        val result = ClosedIntRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals(ClosedIntRange(100, 500), result.get(0))
    }

    @Test
    fun merge_test03() {
        val ranges = ArrayList<ClosedIntRange>()
        ranges.add(ClosedIntRange(10, 30))
        ranges.add(ClosedIntRange(20, 40))
        ranges.add(ClosedIntRange(50, 60))
        ranges.add(ClosedIntRange(55, 65))
        ranges.add(ClosedIntRange(66, 70))
        ranges.add(ClosedIntRange(80, 90))
        val result = ClosedIntRange.mergeAdjacent(ranges).toTypedArray()
        assertEquals(3, result.size.toLong())
        val expected = arrayOf(ClosedIntRange(10, 40), ClosedIntRange(50, 70), ClosedIntRange(80, 90))
        assertArrayEquals(expected, result)
    }
}
