package vladsaif.syncedit.plugin.sound

interface Player : AutoCloseable {

  fun setOnStopAction(action: () -> Unit)

  fun getFramePosition(): Long

  fun play(errorHandler: (Throwable) -> Unit)

  fun resume()

  fun pause()

  fun stop()

  fun stopImmediately()
}