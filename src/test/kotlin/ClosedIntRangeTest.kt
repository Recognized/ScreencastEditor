package vladsaif.syncedit.plugin

import org.junit.Assert.*
import org.junit.Test
import vladsaif.syncedit.plugin.ClosedIntRange.Companion.clr

class ClosedIntRangeTest {

  @Test
  fun `test border intersection`() {
    assertEquals(1, ((40 clr 60) intersect (60 clr 70)).length)
  }

  @Test
  fun `test not intersecting ranges`() {
    assertEquals(0, ((40 clr 60) intersect (80 clr 90)).length)
  }

  @Test
  fun `test intersecting ranges`() {
    assertEquals(11, ((40 clr 60) intersect (50 clr 90)).length)
  }

  @Test
  fun `test not intersecting with infinite range`() {
    assertFalse((0 clr 1000).intersects((1001 clr Int.MAX_VALUE)))
  }

  @Test
  fun `test intersection of both side infinite range`() {
    assertEquals(0 clr 1000, (Int.MIN_VALUE clr Int.MAX_VALUE).intersect(0 clr 1000))
  }

  @Test
  fun `test intersection of two infinite ranges`() {
    assertEquals(
        Int.MIN_VALUE clr Int.MAX_VALUE,
        (Int.MIN_VALUE clr Int.MAX_VALUE).intersect(Int.MIN_VALUE clr Int.MAX_VALUE)
    )
  }

  @Test
  fun `test intersecting with infinite range`() {
    assertTrue((0 clr 100000).intersects((1001 clr Int.MAX_VALUE)))
  }

  @Test
  fun `test merge adjacent ranges into one`() {
    val ranges = mutableListOf<ClosedIntRange>()
    ranges.add(10 clr 30)
    ranges.add(31 clr 40)
    ranges.add(41 clr 50)
    val result = ClosedIntRange.mergeAdjacent(ranges)
    assertEquals(1, result.size.toLong())
    assertEquals((10 clr 50), result.get(0))
  }

  @Test
  fun `test merge intersecting ranges into one`() {
    val ranges = mutableListOf<ClosedIntRange>()
    ranges.add(100 clr 300)
    ranges.add(200 clr 400)
    ranges.add(250 clr 500)
    val result = ClosedIntRange.mergeAdjacent(ranges)
    assertEquals(1, result.size.toLong())
    assertEquals((100 clr 500), result[0])
  }

  @Test
  fun `test merge complex ranges`() {
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
