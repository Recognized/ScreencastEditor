package vladsaif.syncedit.plugin.audioview.waveform.impl

import org.junit.Test
import vladsaif.syncedit.plugin.RESOURCES_PATH
import kotlin.test.assertTrue

class SimpleAudioModelTest {

  private val wav by lazy {
    val wavFile = RESOURCES_PATH.resolve("demo.wav")
    // Throws exception if not supported
    SimpleAudioModel(wavFile)
  }
  private val mp3 by lazy {
    val wavFile = RESOURCES_PATH.resolve("demo.mp3")
    // Throws exception if not supported
    SimpleAudioModel(wavFile)
  }

  @Test
  fun `test wav support`() {
    wav
  }

  @Test
  fun `test mp3 support`() {
    mp3
  }

  @Test
  fun `test wav duration correct`() {
    assertTrue("Actual duration: ${wav.trackDurationMilliseconds}") {
      wav.trackDurationMilliseconds in 23700.0..23900.0
    }
  }

  @Test
  fun `test mp3 duration correct`() {
    assertTrue("Actual duration: ${mp3.trackDurationMilliseconds}") {
      mp3.trackDurationMilliseconds in 23700.0..23900.0
    }
  }

  @Test
  fun `test wav frames are known`() {
    assertTrue {
      wav.totalFrames > 0
    }
  }

  @Test
  fun `test mp3 frames are known`() {
    assertTrue {
      mp3.totalFrames > 0
    }
  }
}