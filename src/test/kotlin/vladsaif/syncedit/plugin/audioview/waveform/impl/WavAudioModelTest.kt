package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.RESOURCES_PATH

class WavAudioModelTest : SimpleAudioModelTestBase() {

  private val wav by lazy {
    val wavFile = RESOURCES_PATH.resolve("demo.wav")
    // Throws exception if not supported
    SimpleAudioModel(wavFile)
  }

  override val audio: SimpleAudioModel
    get() = wav
}