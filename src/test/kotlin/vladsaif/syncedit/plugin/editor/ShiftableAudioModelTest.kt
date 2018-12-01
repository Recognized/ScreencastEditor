package vladsaif.syncedit.plugin.editor

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import vladsaif.syncedit.plugin.RESOURCES_PATH
import vladsaif.syncedit.plugin.editor.audioview.waveform.AveragedSampleData
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.SimpleAudioModel
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ShiftableAudioModelTest(path: Path) {

  private val myAudio = SimpleAudioModel(path)

  @Test
  fun `test frames are known`() {
    assertTrue {
      myAudio.totalFrames > 0
    }
  }

  @Test
  fun `test duration correct`() {
    assertTrue("Actual duration: ${myAudio.trackDurationMilliseconds}") {
      myAudio.trackDurationMilliseconds in 23700.0..23900.0
    }
  }

  @Test
  fun `test data before beginning`() {
    withOffset(1000) {
      val data = getAveragedSampleData(100, 0..9) { true }
      val expected = List(data.size) { AveragedSampleData(0, 0, data.first().sampleSizeInBits) }
      assertEquals(expected, data)
    }
  }

  @Test
  fun `test start data with offset`() {
    val expectedData = myAudio.getAveragedSampleData(100, 0..9) { true }
    withOffset(1000) {
      val data = getAveragedSampleData(100, 10..19) { true }
      assertEquals(expectedData, data)
    }
  }

  @Test
  fun `test start data with negative offset`() {
    val expectedData = myAudio.getAveragedSampleData(100, 10..19) { true }
    withOffset(-1000) {
      val data = getAveragedSampleData(100, 0..9) { true }
      assertEquals(expectedData, data)
    }
  }

  @Test
  fun `test data of all track do not depend on offset`() {
    val data = myAudio.getAveragedSampleData(1000, 0..200) { true }
    withOffset(1000) {
      assertEquals(data, getAveragedSampleData(1000, 1..201) { true })
    }
    withOffset(-1000) {
      assertEquals(data, getAveragedSampleData(1000, -1..199) { true })
    }
  }

  @Test
  fun `test skipped chunks with offset`() {
    withOffset(1000) {
      assertEquals(10, getAveragedSampleData(100, 20..30) { true }.first().skippedChunks)
    }
  }

  private fun withOffset(offset: Long, action: SimpleAudioModel.() -> Unit) {
    myAudio.offsetFrames = offset
    myAudio.action()
    myAudio.offsetFrames = 0
  }

  companion object {

    @Parameterized.Parameters
    @JvmStatic
    fun files() = listOf("demo.mp3", "demo.wav").map { RESOURCES_PATH.resolve(it) }
  }
}
