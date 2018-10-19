package vladsaif.syncedit.plugin.sound

interface Player : AutoCloseable {

  /**
   * Set [updater] that will be sometimes called with the number of frames written to the source data line.
   */
  fun setProcessUpdater(updater: (Long) -> Unit)

  fun setOnStopAction(action: () -> Unit)

  fun play(errorHandler: (Throwable) -> Unit)

  fun resume()

  fun pause()

  fun stop()

  fun stopImmediately()
}