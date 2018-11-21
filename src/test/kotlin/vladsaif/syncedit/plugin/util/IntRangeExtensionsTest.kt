package vladsaif.syncedit.plugin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.test.assertTrue

class IntRangeExtensionsTest {

  @Test
  fun `test border intersection`() {
    assertEquals(1, ((40..60) intersectWith (60..70)).length)
  }

  @Test
  fun `test not intersecting ranges`() {
    assertEquals(0, ((40..60) intersectWith (80..90)).length)
  }

  @Test
  fun `test intersecting ranges`() {
    assertEquals(11, ((40..60) intersectWith (50..90)).length)
  }

  @Test
  fun `test not intersecting with infinite range`() {
    assertFalse((0..1000).intersects((1001..Int.MAX_VALUE)))
  }

  @Test
  fun `test intersection of both side infinite range`() {
    assertEquals(0..1000, (Int.MIN_VALUE..Int.MAX_VALUE).intersectWith(0..1000))
  }

  @Test
  fun `test intersection of two infinite ranges`() {
    assertEquals(
        Int.MIN_VALUE..Int.MAX_VALUE,
        (Int.MIN_VALUE..Int.MAX_VALUE).intersectWith(Int.MIN_VALUE..Int.MAX_VALUE)
    )
  }

  @Test
  fun `test intersecting with infinite range`() {
    assertTrue((0..100000).intersects((1001..Int.MAX_VALUE)))
  }
}
