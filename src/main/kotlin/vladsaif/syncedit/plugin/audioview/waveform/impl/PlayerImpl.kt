package vladsaif.syncedit.plugin.audioview.waveform.impl

import com.intellij.openapi.application.ApplicationManager
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.Player
import vladsaif.syncedit.plugin.audioview.waveform.toDecodeFormat
import java.nio.file.Path
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.min

class PlayerImpl(private val file: Path) : Player {
  private val mySource: SourceDataLine
  private var myProcessUpdater: (Long) -> Unit = {}
  private var mySignalStopReceived = false

  init {
    val fileFormat = AudioSystem.getAudioFileFormat(file.toFile())
    mySource = AudioSystem.getSourceDataLine(fileFormat.format.toDecodeFormat())
  }

  override fun applyEditions(editionModel: EditionModel) {
    AudioSystem.getAudioInputStream(file.toFile()).use {
      val inputStream = it
      AudioSystem.getAudioInputStream(inputStream.format.toDecodeFormat(), inputStream).use {
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
    println(editions)
    outer@ for (edition in editions) {
      var needBytes = edition.first.length * frameSize
      when (edition.second) {
        CUT -> {
          totalFrames += needBytes / frameSize
          myProcessUpdater(totalFrames)
          while (needBytes != 0L && !mySignalStopReceived) {
            val skipped = decodedStream.skip(needBytes)
            needBytes -= skipped
            if (skipped == 0L || mySignalStopReceived) {
              break@outer
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
              totalFrames += zeroesCount / frameSize
              myProcessUpdater(totalFrames)
              needBytes -= zeroesCount
            }
            if (needSkip != 0L) {
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
            writeOrBlock(buffer, read)
            totalFrames += read / frameSize
            myProcessUpdater(totalFrames)
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
    mySource.stop()
    mySource.drain()
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