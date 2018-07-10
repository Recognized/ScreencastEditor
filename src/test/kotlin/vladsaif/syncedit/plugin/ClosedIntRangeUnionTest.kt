package vladsaif.syncedit.plugin

import org.junit.Before
import org.junit.Test


import java.util.ArrayList
import java.util.Random

import org.junit.Assert.*

class ClosedIntRangeUnionTest {
    private val model = ClosedIntRangeUnion()

    @Before
    fun setUpModel() {
        model.clear()
        for (i in 0..9) {
            model.union(ClosedIntRange.from(i * 20, 10))
        }
    }

    @Test
    fun deleting_ranges_test01() {
        for (i in 0..9) {
            model.exclude(ClosedIntRange.from(i * 20, 10))
        }
        assertEquals(0, model.ranges.size)
    }

    @Test
    fun deleting_ranges_test02() {
        for (i in 10 downTo 0) {
            model.exclude(ClosedIntRange.from(i * 20 + 10, 10))
        }
        assertEquals(10, model.ranges.size)
    }

    @Test
    fun deleting_ranges_test03() {
        model.exclude(ClosedIntRange(25, 89))
        assertEquals(7, model.ranges.size)
    }

    @Test
    fun deleting_ranges_test04() {
        model.exclude(ClosedIntRange(10, 59))
        assertEquals(8, model.ranges.size)
    }

    @Test
    fun deleting_ranges_test05() {
        model.clear()
        model.union(ClosedIntRange.from(1, 30))
        assertEquals(1, model.ranges.size)
        model.union(ClosedIntRange.from(0, 50))
        assertEquals(1, model.ranges.size)
        model.union(ClosedIntRange.from(5, 60))
        assertEquals(1, model.ranges.size)
        model.union(ClosedIntRange.from(40, 10))
        assertEquals(1, model.ranges.size)
    }

    @Test
    fun adding_ranges_test01() {
        model.clear()
        model.union(ClosedIntRange.from(1, 30))
        model.exclude(ClosedIntRange.from(5, 5))
        model.exclude(ClosedIntRange.from(12, 5))
        assertEquals(3, model.ranges.size)
    }

    @Test
    fun adding_ranges_test02() {
        model.clear()
        model.union(ClosedIntRange.from(1, 100))
        model.exclude(ClosedIntRange.from(5, 5))
        model.exclude(ClosedIntRange.from(12, 5))
        model.exclude(ClosedIntRange.from(0, 3))
        model.exclude(ClosedIntRange.from(90, 20))
        assertEquals(3, model.ranges.size)
    }

    @Test
    fun impose_test01() {
        model.clear()
        model.union(ClosedIntRange(40, 60))
        assertEquals(ClosedIntRange(30, 39), model.impose(ClosedIntRange(30, 40)))
    }

    @Test
    fun impose_test02() {
        assertEquals(ClosedIntRange(20, 45), model.impose(ClosedIntRange(45, 95)))
    }

    @Test
    fun impose_test03() {
        model.clear()
        model.union(ClosedIntRange(10, 100))
        assertTrue(model.impose(ClosedIntRange(50, 60)).empty)
    }

    @Test
    fun impose_test04() {
        assertEquals(model.impose(ClosedIntRange(40, 60)), model.impose(ClosedIntRange(40, 60)))
    }

    @Test
    fun impose_test05() {
        model.clear()
        val gen = Random(System.currentTimeMillis())
        for (i in 0..99) {
            val rand = ClosedIntRange.from(gen.nextInt() % 300, gen.nextInt() % 300)
            assertEquals(rand, model.impose(rand))
        }
    }

    @Test
    fun impose_cache_test01() {
        val gen = Random(System.currentTimeMillis())
        val ranges = ArrayList<ClosedIntRange>()
        for (i in 0..99) {
            ranges.add(ClosedIntRange.from(gen.nextInt() % 300, gen.nextInt() % 300))
        }
        val resultsWithClear = ArrayList<ClosedIntRange>()
        val resultsNoClear = ArrayList<ClosedIntRange>()
        for (x in ranges) {
            setUpModel()
            resultsWithClear.add(model.impose(x))
        }
        for (x in ranges) {
            resultsNoClear.add(model.impose(x))
        }
        assertArrayEquals(resultsNoClear.toTypedArray(), resultsWithClear.toTypedArray())
    }


}
