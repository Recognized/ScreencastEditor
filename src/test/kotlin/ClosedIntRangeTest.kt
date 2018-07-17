package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.ClosedIntRange.Companion.clr

class ClosedIntRangeTest {

    @Test
    fun intersection_test01() {
        assertEquals(1, ((40 clr 60) intersect (60 clr 70)).length)
    }

    @Test
    fun intersection_test02() {
        assertEquals(0, ((40 clr 60) intersect (80 clr 90)).length)
    }

    @Test
    fun intersection_test03() {
        assertEquals(11, ((40 clr 60) intersect (50 clr 90)).length)
    }

    @Test
    fun intersection_test04() {
        assertFalse((0 clr 1000).intersects((1001 clr Int.MAX_VALUE)))
    }

    @Test
    fun intersection_test05() {
        assertTrue((0 clr 100000).intersects((1001 clr Int.MAX_VALUE)))
    }

    @Test
    fun merge_test01() {
        val ranges = mutableListOf<ClosedIntRange>()
        ranges.add(10 clr 30)
        ranges.add(31 clr 40)
        ranges.add(41 clr 50)
        val result = ClosedIntRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals((10 clr 50), result.get(0))
    }

    @Test
    fun merge_test02() {
        val ranges = mutableListOf<ClosedIntRange>()
        ranges.add(100 clr 300)
        ranges.add(200 clr 400)
        ranges.add(250 clr 500)
        val result = ClosedIntRange.mergeAdjacent(ranges)
        assertEquals(1, result.size.toLong())
        assertEquals((100 clr 500), result[0])
    }

    @Test
    fun merge_test03() {
        val ranges = mutableListOf<ClosedIntRange>()
        ranges.add(10 clr 30)
        ranges.add(20 clr 40)
        ranges.add(50 clr 60)
        ranges.add(55 clr 65)
        ranges.add(66 clr 70)
        ranges.add(80 clr 90)
        val result = ClosedIntRange.mergeAdjacent(ranges).toTypedArray()
        assertEquals(3, result.size.toLong())
        val expected = arrayOf((10 clr 40), (50 clr 70), (80 clr 90))
        assertArrayEquals(expected, result)
    }
}
