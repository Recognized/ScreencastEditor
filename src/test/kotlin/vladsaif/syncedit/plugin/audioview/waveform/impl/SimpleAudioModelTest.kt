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

  private val myAudio = SimpleAudioModel(path)
  private val myChunks = if (myAudio.totalFrames % 4000 == 0L) 3999 else 4000
  private val myFramePerChunk = myAudio.totalFrames / myChunks
  private val myFramePerBigChunk = myFramePerChunk + 1
  private val myExcess = (myAudio.totalFrames % myChunks).toInt()

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
  fun `test zero chunk start frame`() {
    assertEquals(0, myAudio.getStartFrame(myChunks, 0))
    assertEquals(0, myAudio.getChunk(myChunks, 0))
  }

  @Test
  fun `test big chunk start frame`() {
    assertEquals((myExcess - 1) * myFramePerBigChunk, myAudio.getStartFrame(myChunks, myExcess - 1))
    assertEquals(myExcess - 1, myAudio.getChunk(myChunks, myAudio.getStartFrame(myChunks, myExcess - 1)))
  }

  @Test
  fun `test first small chunk start frame`() {
    assertEquals(myExcess * myFramePerBigChunk, myAudio.getStartFrame(myChunks, myExcess))
    assertEquals(myExcess, myAudio.getChunk(myChunks, myAudio.getStartFrame(myChunks, myExcess)))
  }

  @Test
  fun `test second small chunk start frame`() {
    assertEquals(myExcess * myFramePerBigChunk + myFramePerChunk, myAudio.getStartFrame(myChunks, myExcess + 1))
    assertEquals(myExcess + 1, myAudio.getChunk(myChunks, myAudio.getStartFrame(myChunks, myExcess + 1)))
  }

  @Test
  fun `test frame range to milliseconds range`() {
    val range = IRange(500, 1500)
    assertEquals(range, myAudio.frameRangeToMsRange(myAudio.msRangeToFrameRange(range)))
  }

  companion object {

    @Parameterized.Parameters
    @JvmStatic
    fun files() = listOf("demo.mp3", "demo.wav").map { RESOURCES_PATH.resolve(it) }
  }
}
