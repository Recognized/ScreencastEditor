package vladsaif.syncedit.plugin.audioview.waveform.impl

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.RESOURCES_PATH
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class SimpleAudioModelTest(path: Path) {

  private val audio = SimpleAudioModel(path)
  private val chunks = if (audio.totalFrames % 4000 == 0L) 3999 else 4000
  private val framePerChunk = audio.totalFrames / chunks
  private val framePerBigChunk = framePerChunk + 1
  private val excess = (audio.totalFrames % chunks).toInt()

  @Test
  fun `test frames are known`() {
    assertTrue {
      audio.totalFrames > 0
    }
  }

  @Test
  fun `test duration correct`() {
    assertTrue("Actual duration: ${audio.trackDurationMilliseconds}") {
      audio.trackDurationMilliseconds in 23700.0..23900.0
    }
  }

  @Test
  fun `test zero chunk start frame`() {
    assertEquals(0, audio.getStartFrame(chunks, 0))
    assertEquals(0, audio.getChunk(chunks, 0))
  }

  @Test
  fun `test big chunk start frame`() {
    assertEquals((excess - 1) * framePerBigChunk, audio.getStartFrame(chunks, excess - 1))
    assertEquals(excess - 1, audio.getChunk(chunks, audio.getStartFrame(chunks, excess - 1)))
  }

  @Test
  fun `test first small chunk start frame`() {
    assertEquals(excess * framePerBigChunk, audio.getStartFrame(chunks, excess))
    assertEquals(excess, audio.getChunk(chunks, audio.getStartFrame(chunks, excess)))
  }

  @Test
  fun `test second small chunk start frame`() {
    assertEquals(excess * framePerBigChunk + framePerChunk, audio.getStartFrame(chunks, excess + 1))
    assertEquals(excess + 1, audio.getChunk(chunks, audio.getStartFrame(chunks, excess + 1)))
  }

  @Test
  fun `test frame range to milliseconds range`() {
    val range = IRange(500, 1500)
    assertEquals(range, audio.frameRangeToMsRange(audio.msRangeToFrameRange(range)))
  }

  companion object {

    @Parameterized.Parameters
    @JvmStatic
    fun files() = listOf("demo.mp3", "demo.wav").map { RESOURCES_PATH.resolve(it) }
  }
}
