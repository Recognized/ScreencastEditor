package vladsaif.syncedit.plugin.audioview

import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.floorToInt
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.min

/**
 * Working only for formats which sampleSizeInBits >= 8
 */
class AudioSampler(
    val underlyingStream: AudioInputStream,
    skippedFrames: Long,
    readFrames: Long
) : AutoCloseable by underlyingStream {
  val buffer = ByteArray(8192)
  var pos = 0
  var endPos = 0
  var leftBytes = readFrames * frameSizeBytes
  val sampleSizeInBits
    get() = underlyingStream.format.sampleSizeInBits
  val frameSizeBytes
    get() = underlyingStream.format.frameSize

  init {
    if (underlyingStream.format.sampleSizeInBits < 8) {
      throw UnsupportedAudioFileException("Unsupported format, sample size ($sampleSizeInBits bits) less than byte")
    }
    if (underlyingStream is DecodedMpegAudioInputStream) {
      val start = System.currentTimeMillis()
      if (skippedFrames != 0L) {
        underlyingStream.skipFramesMpeg(buffer, skippedFrames)
      }
    } else {
      if (skippedFrames != 0L) {
        underlyingStream.skipFrames(skippedFrames)
      }
    }
  }

  constructor(underlyingStream: AudioInputStream) : this(underlyingStream, 0, 0) {
    leftBytes = Long.MAX_VALUE
  }

  /**
   * Iterate over each sample in this stream.
   *
   * Samples are ordered firstly by frames, then by channels.
   */
  inline fun forEachSample(consumer: (Long) -> Unit) {
    while (leftBytes != 0L) {
      if (pos == endPos) {
        System.arraycopy(buffer, pos, buffer, 0, endPos - pos)
        endPos -= pos
        pos = 0
        do {
          val ret = underlyingStream.read(
              buffer,
              endPos,
              min(min(leftBytes, Int.MAX_VALUE.toLong()).toInt(), buffer.size - endPos)
          )
          if (ret == -1) break
          endPos += ret
        } while (ret == 0)
      }
      if (pos == endPos) break
      var bitPos = 0
      for (j in 1..frameSizeBytes * 8 / sampleSizeInBits) {
        var leftBits = sampleSizeInBits
        var result = 0L
        if (bitPos != 0) {
          result = result or ((buffer[pos++].toLong() and 0b1111_1111) shr bitPos)
          leftBits -= bitPos
        }
        for (i in 1..leftBits / 8) {
          result = result or ((buffer[pos++].toLong() and 0b1111_1111) shl (sampleSizeInBits - leftBits))
          leftBits -= 8
        }
        bitPos = leftBits
        if (leftBits != 0) {
          result = result or (((buffer[pos].toLong() and ((1L shl leftBits) - 1))) shl (sampleSizeInBits - leftBits))
        }
        val extensionBits = 64 - sampleSizeInBits
        consumer((result shl extensionBits) shr extensionBits)
      }
      leftBytes -= frameSizeBytes
    }
  }
}

fun AudioInputStream.skipFrames(count: Long) {
  val bytesToSkip = count * format.frameSize
  var skippedBytes = 0L
  while (bytesToSkip != skippedBytes) {
    skippedBytes += skip(bytesToSkip - skippedBytes)
  }
}

fun DecodedMpegAudioInputStream.skipFramesMpeg(buffer: ByteArray, count: Long) {
  var bytesToSkip = count * format.frameSize
  while (bytesToSkip != 0L) {
    val skipped = read(buffer, 0, min(bytesToSkip.floorToInt(), buffer.size))
    bytesToSkip -= skipped
  }
}
