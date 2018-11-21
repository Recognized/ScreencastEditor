package vladsaif.syncedit.plugin.editor

import org.junit.Test
import kotlin.test.assertEquals

class CoordinatorTest {
  private val coordinator = Coordinator()

  init {
    coordinator.framesPerPixel = 100
    coordinator.visibleRange = 0..1000
    coordinator.framesPerSecond = 1000
  }

  @Test
  fun `test zero chunk start frame`() {
    assertEquals(0, coordinator.toFrame(0))
    assertEquals(0, coordinator.toPixel(0L))
  }

  @Test
  fun `test frame range to screen pixel range`() {
    val range = IntRange(500, 1500)
    assertEquals(50000L..150000L, coordinator.toFrameRange(range))
  }

  @Test
  fun `test negative frames to negative pixels`() {
    assertEquals(-1, coordinator.toPixel(-1))
  }
}