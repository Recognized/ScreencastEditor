package vladsaif.syncedit.plugin.audioview.waveform.impl

import org.junit.Assert.assertEquals
import org.junit.Test
import vladsaif.syncedit.plugin.LRange
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel

class DefaultEditionModelTest {

  @Test
  fun `test equals method`() {
    assertEquals(DefaultEditionModel(), DefaultEditionModel())
    val range1 = LRange(10, 20)
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
      cut(LRange(0, 200))
      cut(LRange(400, 600))
      cut(LRange(800, 900))
      mute(LRange(1000, 1200))
      mute(LRange(150, 450))
      mute(LRange(10000, 40000))
      undo(LRange(12000, 35000))
    }
    assertEquals(model, EditionModel.deserialize(model.serialize()))
  }
}