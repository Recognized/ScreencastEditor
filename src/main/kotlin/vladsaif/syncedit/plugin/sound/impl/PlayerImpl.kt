package vladsaif.syncedit.plugin.sound.impl

import com.intellij.openapi.application.ApplicationManager
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.editor.audioview.skipFramesMpeg
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.sound.Player
import vladsaif.syncedit.plugin.sound.SoundProvider
import vladsaif.syncedit.plugin.util.length
import java.io.InputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.min

class PlayerImpl(
  private val getAudioStream: () -> InputStream,
  private val editionModel: EditionModel
) : Player {
  private val mySource: SourceDataLine
  private var myOnStopAction: () -> Unit = {}
  private var mySignalStopReceived = false

  init {
    val fileFormat = SoundProvider.getAudioFileFormat(getAudioStream().buffered())
    mySource = AudioSystem.getSourceDataLine(fileFormat.format)
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
    val editions = editionModel.editions
    if (!mySource.isOpen) mySource.open(decodedStream.format)
    ApplicationManager.getApplication().invokeLater { mySource.start() }
    val frameSize = decodedStream.format.frameSize
    val buffer = ByteArray(1 shl 14)
    outer@ for (edition in editions) {
      var needBytes = edition.first.length * frameSize
      when (edition.second) {
        CUT -> {
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
        MUTE -> {
          buffer.fill(0)
          var needSkip = needBytes
          while (needBytes != 0L || needSkip != 0L) {
            if (needBytes != 0L) {
              val zeroesCount = min(buffer.size.toLong(), needBytes)
                .toInt()
                .modFloor(frameSize)
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
        NO_CHANGES -> {
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

fun Int.modFloor(modulus: Int): Int {
  return this - this % modulus
}
