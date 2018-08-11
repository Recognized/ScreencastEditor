package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.audioview.waveform.impl.PlayerImpl
import java.nio.file.Path

interface Player : AutoCloseable {

  /**
   * Applies editions from [editionModel] to the underlying audio data.
   *
   * @throws javax.sound.sampled.LineUnavailableException if the [source] line cannot be
   * opened due to resource restrictions.
   * @throws SecurityException if the line cannot be
   * opened due to security restrictions.
   */
  fun applyEditions(editionModel: EditionModel)

  /**
   * Set [updater] that will be sometimes called with the number of frames written to the source data line.
   */
  fun setProcessUpdater(updater: (Long) -> Unit)

  fun play()

  fun pause()

  fun stop()

  fun stopImmediately()

  companion object {
    fun create(file: Path): Player = PlayerImpl(file)
  }

  enum class PlayState {
    PAUSE, STOP, PLAY
  }
}