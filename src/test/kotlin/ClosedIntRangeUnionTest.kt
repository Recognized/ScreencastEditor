package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.ClosedIntRange.Companion.clr
import java.util.*

class ClosedIntRangeUnionTest {
    private fun getTestModel(): ClosedIntRangeUnion {
        val model = ClosedIntRangeUnion()
        model.clear()
        for (i in 0..9) {
            model.union(ClosedIntRange.from(i * 20, 10))
        }
        return model
    }

    @Test
    fun union_ranges_test01() {
        val model = getTestModel()
        for (i in 0..9) {
            model.exclude(ClosedIntRange.from(i * 20, 10))
        }
        assertEquals(0, model.ranges.size)
    }

    @Test
    fun union_ranges_test02() {
        val model = getTestModel()
        for (i in 10 downTo 0) {
            model.exclude(ClosedIntRange.from(i * 20 + 10, 10))
        }
        assertEquals(10, model.ranges.size)
    }

    @Test
    fun union_ranges_test03() {
        val model = getTestModel()
        model.exclude(ClosedIntRange(25, 89))
        assertEquals(7, model.ranges.size)
    }

    @Test
    fun union_ranges_test04() {
        val model = getTestModel()
        model.exclude(ClosedIntRange(10, 59))
        assertEquals(8, model.ranges.size)
    }

    @Test
    fun union_ranges_test05() {
        val model = getTestModel()
        val added = -100 clr 100000
        model.union(added)
        assertEquals(1, model.ranges.size)
        assertEquals(added, model.ranges[0])
    }

    @Test
    fun union_ranges_test06() {
        val model = ClosedIntRangeUnion()
        model.union(0 clr 10)
        model.union(20 clr 30)
        model.union(9 clr 29)
        assertEquals(1, model.ranges.size)
        assertEquals(0 clr 30, model.ranges[0])
    }

    @Test
    fun union_ranges_test07() {
        val model = getTestModel()
        model.union(10 clr 179)
        assertEquals(1, model.ranges.size)
        assertEquals(0 clr 189, model.ranges[0])
    }

    @Test
    fun excluding_ranges_test05() {
        val model = ClosedIntRangeUnion()
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
    fun excluding_ranges_test01() {
        val model = ClosedIntRangeUnion()
        model.union(ClosedIntRange.from(1, 30))
        model.exclude(ClosedIntRange.from(5, 5))
        model.exclude(ClosedIntRange.from(12, 5))
        assertEquals(3, model.ranges.size)
    }

    @Test
    fun excluding_ranges_test02() {
        val model = ClosedIntRangeUnion()
        model.union(ClosedIntRange.from(1, 100))
        model.exclude(ClosedIntRange.from(5, 5))
        model.exclude(ClosedIntRange.from(12, 5))
        model.exclude(ClosedIntRange.from(0, 3))
        model.exclude(ClosedIntRange.from(90, 20))
        assertEquals(3, model.ranges.size)
    }

    @Test
    fun impose_test01() {
        val model = ClosedIntRangeUnion()
        model.union(ClosedIntRange(40, 60))
        assertEquals(ClosedIntRange(30, 39), model.impose(ClosedIntRange(30, 40)))
    }

    @Test
    fun impose_test02() {
        val model = getTestModel()
        assertEquals(ClosedIntRange(20, 45), model.impose(ClosedIntRange(45, 95)))
    }

    @Test
    fun impose_test03() {
        val model = ClosedIntRangeUnion()
        model.union(ClosedIntRange(10, 100))
        assertTrue(model.impose(ClosedIntRange(50, 60)).empty)
    }

    @Test
    fun impose_test04() {
        val model = ClosedIntRangeUnion()
        assertEquals(model.impose(ClosedIntRange(40, 60)), model.impose(ClosedIntRange(40, 60)))
    }

    @Test
    fun impose_test05() {
        val model = ClosedIntRangeUnion()
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
            val model = getTestModel()
            resultsWithClear.add(model.impose(x))
        }
        val model = getTestModel()
        for (x in ranges) {
            resultsNoClear.add(model.impose(x))
        }
        assertArrayEquals(resultsNoClear.toTypedArray(), resultsWithClear.toTypedArray())
    }


}
