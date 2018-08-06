package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.ClosedIntRange.Companion.clr
import java.util.*

class ClosedIntRangeUnionTest {
  private fun createTestModel(): ClosedIntRangeUnion {
    val model = ClosedIntRangeUnion()
    model.clear()
    for (i in 0..9) {
      model.union(ClosedIntRange.from(i * 20, 10))
    }
    return model
  }

  @Test
  fun `test exclude all ranges`() {
    val model = createTestModel()
    for (i in 0..9) {
      model.exclude(ClosedIntRange.from(i * 20, 10))
    }
    assertEquals(0, model.ranges.size)
  }

  @Test
  fun `test exclude one unit from each range`() {
    val model = createTestModel()
    for (i in 10 downTo 0) {
      model.exclude(ClosedIntRange.from(i * 20 + 10, 10))
    }
    assertEquals(10, model.ranges.size)
  }

  @Test
  fun `test exclude range that cover three ranges in model`() {
    val model = createTestModel()
    model.exclude(ClosedIntRange(25, 89))
    assertEquals(7, model.ranges.size)
  }

  @Test
  fun `test exclude part of ranges`() {
    val model = createTestModel()
    model.exclude(ClosedIntRange(10, 59))
    assertEquals(8, model.ranges.size)
  }

  @Test
  fun `test add range that covers all ranges`() {
    val model = createTestModel()
    val added = -100 clr 100000
    model.union(added)
    assertEquals(1, model.ranges.size)
    assertEquals(added, model.ranges[0])
  }

  @Test
  fun `test add intersecting ranges`() {
    val model = ClosedIntRangeUnion()
    model.union(0 clr 10)
    model.union(20 clr 30)
    model.union(9 clr 29)
    assertEquals(1, model.ranges.size)
    assertEquals(0 clr 30, model.ranges[0])
  }

  @Test
  fun `test fill spaces between ranges`() {
    val model = createTestModel()
    model.union(10 clr 179)
    assertEquals(1, model.ranges.size)
    assertEquals(0 clr 189, model.ranges[0])
  }

  @Test
  fun `test add intersecting ranges incrementally`() {
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
  fun `test divide range by excluding center part`() {
    val model = ClosedIntRangeUnion()
    model.union(ClosedIntRange.from(1, 30))
    model.exclude(ClosedIntRange.from(5, 5))
    model.exclude(ClosedIntRange.from(12, 5))
    assertEquals(3, model.ranges.size)
  }

  @Test
  fun `test exclude outer and inner parts from range`() {
    val model = ClosedIntRangeUnion()
    model.union(ClosedIntRange.from(1, 100))
    model.exclude(ClosedIntRange.from(5, 5))
    model.exclude(ClosedIntRange.from(12, 5))
    model.exclude(ClosedIntRange.from(0, 3))
    model.exclude(ClosedIntRange.from(90, 20))
    assertEquals(3, model.ranges.size)
  }

  @Test
  fun `test impose on border`() {
    val model = ClosedIntRangeUnion()
    model.union(ClosedIntRange(40, 60))
    assertEquals(ClosedIntRange(30, 39), model.impose(ClosedIntRange(30, 40)))
  }

  @Test
  fun `test impose on multiple ranges`() {
    val model = createTestModel()
    assertEquals(ClosedIntRange(20, 45), model.impose(ClosedIntRange(45, 95)))
  }

  @Test
  fun `test impose on containing range`() {
    val model = ClosedIntRangeUnion()
    model.union(ClosedIntRange(10, 100))
    assertTrue(model.impose(ClosedIntRange(50, 60)).empty)
  }

  @Test
  fun `test impose on no ranges`() {
    val model = ClosedIntRangeUnion()
    assertEquals(model.impose(ClosedIntRange(40, 60)), model.impose(ClosedIntRange(40, 60)))
  }

  @Test
  fun `test impose on empty model results in same range`() {
    val model = ClosedIntRangeUnion()
    val gen = Random(System.currentTimeMillis())
    for (i in 0..99) {
      val rand = ClosedIntRange.from(gen.nextInt() % 300, gen.nextInt() % 300)
      assertEquals(rand, model.impose(rand))
    }
  }

  @Test
  fun `test impose cache`() {
    val gen = Random(System.currentTimeMillis())
    val ranges = ArrayList<ClosedIntRange>()
    for (i in 0..99) {
      ranges.add(ClosedIntRange.from(gen.nextInt() % 300, gen.nextInt() % 300))
    }
    val resultsWithClear = ArrayList<ClosedIntRange>()
    val resultsNoClear = ArrayList<ClosedIntRange>()
    for (x in ranges) {
      val model = createTestModel()
      resultsWithClear.add(model.impose(x))
    }
    val model = createTestModel()
    for (x in ranges) {
      resultsNoClear.add(model.impose(x))
    }
    assertArrayEquals(resultsNoClear.toTypedArray(), resultsWithClear.toTypedArray())
  }
}
