package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.IRange.Companion.clr
import java.util.*

class IRangeUnionTest {
  private fun createTestModel(): IRangeUnion {
    val model = IRangeUnion()
    model.clear()
    for (i in 0..9) {
      model.union(IRange.from(i * 20, 10))
    }
    return model
  }

  @Test
  fun `test exclude all ranges`() {
    val model = createTestModel()
    for (i in 0..9) {
      model.exclude(IRange.from(i * 20, 10))
    }
    assertEquals(0, model.ranges.size)
  }

  @Test
  fun `test exclude one unit from each range`() {
    val model = createTestModel()
    for (i in 10 downTo 0) {
      model.exclude(IRange.from(i * 20 + 10, 10))
    }
    assertEquals(10, model.ranges.size)
  }

  @Test
  fun `test exclude range that cover three ranges in model`() {
    val model = createTestModel()
    model.exclude(IRange(25, 89))
    assertEquals(7, model.ranges.size)
  }

  @Test
  fun `test exclude part of ranges`() {
    val model = createTestModel()
    model.exclude(IRange(10, 59))
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
    val model = IRangeUnion()
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
    val model = IRangeUnion()
    model.union(IRange.from(1, 30))
    assertEquals(1, model.ranges.size)
    model.union(IRange.from(0, 50))
    assertEquals(1, model.ranges.size)
    model.union(IRange.from(5, 60))
    assertEquals(1, model.ranges.size)
    model.union(IRange.from(40, 10))
    assertEquals(1, model.ranges.size)
  }

  @Test
  fun `test divide range by excluding center part`() {
    val model = IRangeUnion()
    model.union(IRange.from(1, 30))
    model.exclude(IRange.from(5, 5))
    model.exclude(IRange.from(12, 5))
    assertEquals(3, model.ranges.size)
  }

  @Test
  fun `test exclude outer and inner parts from range`() {
    val model = IRangeUnion()
    model.union(IRange.from(1, 100))
    model.exclude(IRange.from(5, 5))
    model.exclude(IRange.from(12, 5))
    model.exclude(IRange.from(0, 3))
    model.exclude(IRange.from(90, 20))
    assertEquals(3, model.ranges.size)
  }

  @Test
  fun `test impose on border`() {
    val model = IRangeUnion()
    model.union(IRange(40, 60))
    assertEquals(IRange(30, 39), model.impose(IRange(30, 40)))
  }

  @Test
  fun `test impose on multiple ranges`() {
    val model = createTestModel()
    assertEquals(IRange(20, 45), model.impose(IRange(45, 95)))
  }

  @Test
  fun `test impose on containing range`() {
    val model = IRangeUnion()
    model.union(IRange(10, 100))
    assertTrue(model.impose(IRange(50, 60)).empty)
  }

  @Test
  fun `test impose on no ranges`() {
    val model = IRangeUnion()
    assertEquals(model.impose(IRange(40, 60)), model.impose(IRange(40, 60)))
  }

  @Test
  fun `test impose on empty model results in same range`() {
    val model = IRangeUnion()
    val gen = Random(System.currentTimeMillis())
    for (i in 0..99) {
      val rand = IRange.from(gen.nextInt() % 300, gen.nextInt() % 300)
      assertEquals(rand, model.impose(rand))
    }
  }

  @Test
  fun `test impose cache`() {
    val gen = Random(System.currentTimeMillis())
    val ranges = ArrayList<IRange>()
    for (i in 0..99) {
      ranges.add(IRange.from(gen.nextInt() % 300, gen.nextInt() % 300))
    }
    val resultsWithClear = ArrayList<IRange>()
    val resultsNoClear = ArrayList<IRange>()
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
