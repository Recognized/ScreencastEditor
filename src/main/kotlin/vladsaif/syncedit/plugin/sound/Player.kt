package vladsaif.syncedit.plugin.sound

import com.intellij.openapi.application.ApplicationManager
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.editor.audioview.skipFramesMpeg
import vladsaif.syncedit.plugin.util.length
import vladsaif.syncedit.plugin.util.modFloor
import java.io.InputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.min

interface Player : AutoCloseable {

  fun setOnStopAction(action: () -> Unit)

  fun getFramePosition(): Long

  fun play(errorHandler: (Throwable) -> Unit)

  fun resume()

  fun pause()

  fun stop()

  fun stopImmediately()

  companion object {

    fun create(
      editions: EditionsView,
      offsetFrames: Long,
      getAudioStream: () -> InputStream
    ): Player {
      return PlayerImpl(editions, offsetFrames, getAudioStream)
    }
  }

  private class PlayerImpl(
    editions: EditionsView,
    private val offsetFrames: Long,
    private val getAudioStream: () -> InputStream
  ) : Player {
    private val mySource: SourceDataLine
    private var myOnStopAction: () -> Unit = {}
    private var mySignalStopReceived = false
    private val myEditionModel = editions.copy()

    init {
      val fileFormat = SoundProvider.getAudioFileFormat(getAudioStream().buffered())
      mySource = AudioSystem.getSourceDataLine(fileFormat.format)
      if (offsetFrames < 0) {
        myEditionModel.cut(0 until -offsetFrames)
      }
    }

    override fun pause() {
      mySource.stop()
    }

    override fun stop() {
      mySignalStopReceived = true
      mySource.drain()
      mySource.flush()
      mySource.stop()
    }

    override fun stopImmediately() {
      mySignalStopReceived = true
      mySource.stop()
      mySource.flush()
    }

    override fun close() {
      try {
        mySource.close()
      } catch (ex: Throwable) {
      }
    }

    override fun resume() {
      mySource.start()
    }

    override fun setOnStopAction(action: () -> Unit) {
      myOnStopAction = action
    }

    override fun getFramePosition(): Long {
      return mySource.longFramePosition
    }

    override fun play(errorHandler: (Throwable) -> Unit) {
      mySource.start()
      thread(start = true) {
        SoundProvider.getAudioInputStream(getAudioStream()).use { inputStream ->
          try {
            applyEditionImpl(inputStream)
          } catch (ex: Throwable) {
            ApplicationManager.getApplication().invokeLater { errorHandler(ex) }
          } finally {
            myOnStopAction()
          }
        }
      }
    }

    private fun applyEditionImpl(decodedStream: AudioInputStream) {
      val editions = myEditionModel.editionsModel
      if (!mySource.isOpen) {
        mySource.open(decodedStream.format)
      }
      ApplicationManager.getApplication().invokeLater { mySource.start() }
      val buffer = ByteArray(1 shl 14)
      val frameSize = decodedStream.format.frameSize
      if (offsetFrames > 0) {
        buffer.fill(0)
        var needBytes = offsetFrames * frameSize
        while (needBytes != 0L) {
          val zeroesCount = min(buffer.size.toLong(), needBytes).toInt().modFloor(frameSize)
          writeOrBlock(buffer, zeroesCount)
          needBytes -= zeroesCount
        }
      }
      outer@ for (edition in editions) {
        var needBytes = edition.first.length * frameSize
        when (edition.second) {
          EditionsModel.EditionType.CUT -> {
            while (needBytes != 0L && !mySignalStopReceived) {
              if (decodedStream is DecodedMpegAudioInputStream) {
                decodedStream.skipFramesMpeg(buffer, edition.first.length)
                needBytes = 0L
              } else {
                val skipped = decodedStream.skip(needBytes)
                needBytes -= skipped
                if (skipped == 0L || mySignalStopReceived) {
                  break@outer
                }
              }
            }
          }
          EditionsModel.EditionType.MUTE -> {
            buffer.fill(0)
            var needSkip = needBytes
            while (needBytes != 0L || needSkip != 0L) {
              if (needBytes != 0L) {
                val zeroesCount = min(buffer.size.toLong(), needBytes).toInt().modFloor(frameSize)
                if (mySignalStopReceived) {
                  break@outer
                }
                writeOrBlock(buffer, zeroesCount)
                needBytes -= zeroesCount
              }
              if (needSkip != 0L) {
                if (decodedStream is DecodedMpegAudioInputStream) {
                  decodedStream.skipFramesMpeg(buffer, edition.first.length)
                  needSkip = 0L
                  buffer.fill(0)
                  continue
                }
                val skipped = decodedStream.skip(needSkip)
                needSkip -= skipped
                if (skipped == 0L) {
                  break@outer
                }
              }
            }
          }
          EditionsModel.EditionType.NO_CHANGES -> {
            while (needBytes != 0L) {
              val read = decodedStream.read(buffer, 0, min(buffer.size.toLong(), needBytes).toInt())
              if (read == -1 || mySignalStopReceived) {
                break@outer
              }
              needBytes -= read
              writeOrBlock(buffer, read)
            }
          }
        }
      }
    }

    private fun writeOrBlock(buffer: ByteArray, size: Int) {
      var needWrite = size
      while (needWrite != 0) {
        val written = mySource.write(buffer, size - needWrite, needWrite)
        needWrite -= written
      }
    }
  }
}
