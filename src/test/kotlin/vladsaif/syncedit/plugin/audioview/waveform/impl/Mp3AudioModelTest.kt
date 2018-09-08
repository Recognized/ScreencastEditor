package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.RESOURCES_PATH

class Mp3AudioModelTest : SimpleAudioModelTestBase() {

  private val mp3 by lazy {
    val mp3File = RESOURCES_PATH.resolve("demo.mp3")
    // Throws exception if not supported
    SimpleAudioModel(mp3File)
  }

  override val audio: SimpleAudioModel
    get() = mp3
}