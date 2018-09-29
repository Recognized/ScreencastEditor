package vladsaif.syncedit.plugin.audioview.waveform.impl

import com.intellij.openapi.application.ApplicationManager
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.SoundProvider
import vladsaif.syncedit.plugin.audioview.skipFramesMpeg
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.Player
import vladsaif.syncedit.plugin.audioview.waveform.toDecodeFormat
import java.io.InputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.min

class PlayerImpl(private val getAudioStream: () -> InputStream) : Player {
  private val mySource: SourceDataLine
  private var myProcessUpdater: (Long) -> Unit = {}
  private var mySignalStopReceived = false

  init {
    val fileFormat = SoundProvider.getAudioFileFormat(getAudioStream().buffered())
    mySource = AudioSystem.getSourceDataLine(fileFormat.format.toDecodeFormat())
  }

  override fun applyEditions(editionModel: EditionModel) {
    SoundProvider.getAudioInputStream(getAudioStream().buffered()).use { inputStream ->
      SoundProvider.getAudioInputStream(inputStream.format.toDecodeFormat(), inputStream).use {
        applyEditionImpl(it, editionModel)
      }
    }
  }

  private fun applyEditionImpl(decodedStream: AudioInputStream, editionModel: EditionModel) {
    val editions = editionModel.editions
    if (!mySource.isOpen) mySource.open(decodedStream.format)
    ApplicationManager.getApplication().invokeLater { mySource.start() }
    val frameSize = decodedStream.format.frameSize
    val buffer = ByteArray(1 shl 14)
    var totalFrames = 0L
    outer@ for (edition in editions) {
      var needBytes = edition.first.length * frameSize
      when (edition.second) {
        CUT -> {
          totalFrames += needBytes / frameSize
          myProcessUpdater(totalFrames)
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
              myProcessUpdater(totalFrames)
              writeOrBlock(buffer, zeroesCount)
              totalFrames += zeroesCount / frameSize
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
              println("Break no changes $mySignalStopReceived")
              break@outer
            }
            needBytes -= read
            myProcessUpdater(totalFrames)
            writeOrBlock(buffer, read)
            totalFrames += read / frameSize
          }
        }
      }
    }
    println(totalFrames / decodedStream.format.frameRate)
  }

  private fun writeOrBlock(buffer: ByteArray, size: Int) {
    var needWrite = size
    while (needWrite != 0) {
      val written = mySource.write(buffer, size - needWrite, needWrite)
      needWrite -= written
    }
  }

  /**
   * Set [updater] that will be sometimes called with the number of frames written to the source data line.
   */
  override fun setProcessUpdater(updater: (Long) -> Unit) {
    myProcessUpdater = updater
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

  override fun play() {
    mySource.start()
  }
}

fun Int.modFloor(modulus: Int): Int {
  return this - this % modulus
}