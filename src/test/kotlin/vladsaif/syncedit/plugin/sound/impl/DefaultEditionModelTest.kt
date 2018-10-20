package vladsaif.syncedit.plugin.sound.impl

import org.junit.Assert.assertEquals
import org.junit.Test
import vladsaif.syncedit.plugin.sound.EditionModel

class DefaultEditionModelTest {

  @Test
  fun `test equals method`() {
    assertEquals(DefaultEditionModel(), DefaultEditionModel())
    val range1 = 10L..20L
    assertEquals(DefaultEditionModel().apply { cut(range1) }, DefaultEditionModel().apply { cut(range1) })
    assertEquals(DefaultEditionModel().apply { mute(range1) }, DefaultEditionModel().apply { mute(range1) })
    assertEquals(DefaultEditionModel().apply { cut(range1); undo(range1) }, DefaultEditionModel())
    assertEquals(DefaultEditionModel().apply { mute(range1); undo(range1) }, DefaultEditionModel())
    assertEquals(
        DefaultEditionModel().apply { mute(range1); cut(range1) },
        DefaultEditionModel().apply { cut(range1) }
    )
  }

  @Test
  fun `test serialization`() {
    val model = DefaultEditionModel().apply {
      cut(0L..200L)
      cut(400L..600L)
      cut(800L..900L)
      mute(1000L..1200L)
      mute(150L..450L)
      mute(10000L..40000L)
      undo(12000L..35000L)
    }
    assertEquals(model, EditionModel.deserialize(model.serialize()))
  }
}