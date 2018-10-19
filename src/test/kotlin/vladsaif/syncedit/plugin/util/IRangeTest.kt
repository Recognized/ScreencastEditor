package vladsaif.syncedit.plugin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import vladsaif.syncedit.plugin.util.IRange.Companion.clr
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
}
