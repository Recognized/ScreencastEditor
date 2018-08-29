package vladsaif.syncedit.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import vladsaif.syncedit.plugin.IRange.Companion.clr
import kotlin.test.assertTrue

class IRangeTest {

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
  fun `test intersect itself`() {
    val left = listOf(
        IRange(1, 2),
        IRange(3, 4),
        IRange(5, 6),
        IRange(7, 8)
    )
    val expected = left.associate { it to (it to it) }
    val result = intersect(left, left) { a, b -> a.intersects(b) }
    assertEquals(expected, result)
  }

  @Test
  fun `test none intersects`() {
    val left = listOf(
        IRange(1, 2),
        IRange(2, 3),
        IRange(3, 4),
        IRange(4, 5)
    )
    val right = listOf(
        IRange(10, 20),
        IRange(20, 30),
        IRange(30, 40),
        IRange(40, 50)
    )
    assertEquals(emptyMap<Any, Any>(), intersect(left, right) { a, b -> a.intersects(b) })
  }

  @Test
  fun `test normal intersection`() {
    val left = listOf(
        IRange(10, 20),
        IRange(20, 30),
        IRange(30, 40),
        IRange(40, 50)
    )
    val right = listOf(
        IRange(5, 15),
        IRange(17, 18),
        IRange(25, 45),
        IRange(55, 60)
    )
    val expected = mapOf(
        IRange(10, 20) to (IRange(5, 15) to IRange(17, 18)),
        IRange(20, 30) to (IRange(25, 45) to IRange(25, 45)),
        IRange(30, 40) to (IRange(25, 45) to IRange(25, 45)),
        IRange(40, 50) to (IRange(25, 45) to IRange(25, 45))
    )
    assertEquals(expected, intersect(left, right) { a, b -> a.intersects(b) })
  }
}
