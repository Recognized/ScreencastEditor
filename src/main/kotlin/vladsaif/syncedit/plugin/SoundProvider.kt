package vladsaif.syncedit.plugin

import com.intellij.openapi.diagnostic.logger
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.*
import kotlin.concurrent.thread

/** This object duplicates some part of [javax.sound.sampled.AudioSystem].
 *
 * It exists because of some unknown reasons in Intellij Platform and bad SPI design,
 * which does not allow to manually load implementation classes from code,
 * but only to load them from reading specially formatted files in META-INF/services directory.
 */
object SoundProvider {
  private val LOG = logger<SoundProvider>()
  private val MPEG_PROVIDER = MpegFormatConversionProvider()
  private val MPEG_FILE_READER = MpegAudioFileReader()

  @Throws(java.io.IOException::class, UnsupportedAudioFileException::class)
  fun getAudioInputStream(file: File): AudioInputStream {
    return try {
      AudioSystem.getAudioInputStream(file)
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioInputStream(file)
    }
  }

  @Throws(java.io.IOException::class, UnsupportedAudioFileException::class)
  fun getAudioInputStream(inputStream: InputStream): AudioInputStream {
    return try {
      AudioSystem.getAudioInputStream(inputStream.buffered())
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioInputStream(inputStream.buffered())
    }
  }

  fun isConversionSupported(targetFormat: AudioFormat, sourceFormat: AudioFormat): Boolean {
    return AudioSystem.isConversionSupported(targetFormat, sourceFormat)
        || MPEG_PROVIDER.isConversionSupported(targetFormat, sourceFormat)
  }

  /**
   * @throws IllegalArgumentException if conversion is not supported.
   */
  @Throws(IllegalArgumentException::class)
  fun getAudioInputStream(targetFormat: AudioFormat, stream: AudioInputStream): AudioInputStream {
    return if (MPEG_PROVIDER.isConversionSupported(targetFormat, stream.format)) {
      MPEG_PROVIDER.getAudioInputStream(targetFormat, stream)
    } else AudioSystem.getAudioInputStream(targetFormat, stream)
  }

  @Throws(java.io.IOException::class, UnsupportedAudioFileException::class)
  fun getAudioFileFormat(file: File): AudioFileFormat {
    return try {
      AudioSystem.getAudioFileFormat(file)
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioFileFormat(file)
    }
  }

  @Throws(java.io.IOException::class, UnsupportedAudioFileException::class)
  fun getAudioFileFormat(inputStream: InputStream): AudioFileFormat {
    return try {
      AudioSystem.getAudioFileFormat(inputStream.buffered())
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioFileFormat(inputStream.buffered())
    }
  }

  fun <T> withWavFileStream(file: Path, block: (InputStream) -> T): T {
    val (stream, lock) = convertAudioLazy(file)
    LOG.info("Audio is converting: $file")
    stream.use {
      try {
        return block(it)
      } finally {
        lock.unlock()
        LOG.info("Audio converted: $file")
      }
    }
  }

  private fun AudioFormat.toMonoFormat(): AudioFormat {
    return AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        if (this.sampleRate > 0) this.sampleRate else 44100f,
        16,
        1,
        2,
        if (this.sampleRate > 0) this.sampleRate else 44100f,
        false
    )
  }

  private fun AudioFormat.toPcmPreservingChannels() =
      AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
          44100f,
          16,
          channels,
          2 * channels,
          44100f,
          false)

  // This is needed to make possible converting audio to recognizable WAV format without temporary file storage
  // and without storing whole decoded file in RAM, because it can be very big.
  // Idea of this code is just to convert and send audio stream on the fly,
  // but there are several tricks are needed to implement it.
  private fun convertAudioLazy(file: Path): Pair<InputStream, Lock> {
    val length = countFrames(file)
    val pipeIn = PipedInputStream(1 shl 14)
    val pipeOut = PipedOutputStream(pipeIn)
    val keepAlive = ReentrantLock()
    keepAlive.lock()
    thread {
      Files.newInputStream(file).use { source ->
        // This is needed because of lack of transitive closure in audio system conversions.
        // We need to manually convert audio through intermediate formats.
        withMonoPcmStream(source) { mono ->
          // Lets convert it using pipe
          // No need for join, because execution continue after everything is written to pipe and then thread ends.
          pipeOut.use {
            AudioSystem.write(createSizedAudioStream(mono, length), AudioFileFormat.Type.WAVE, it)
          }
          LOG.info("Data is written to pipe.")
          keepAlive.lock()
        }
      }
    }
    return BufferedInputStream(pipeIn) to keepAlive
  }

  private fun withMonoPcmStream(source: InputStream, action: (AudioInputStream) -> Unit) {
    getAudioInputStream(source.buffered()).use { encoded ->
      when {
        encoded.format == encoded.format.toMonoFormat() -> {
          action(encoded)
        }
        encoded.format == encoded.format.toPcmPreservingChannels() -> {
          getAudioInputStream(encoded.format.toMonoFormat(), encoded).use(action)
        }
        else -> {
          getAudioInputStream(encoded.format.toPcmPreservingChannels(), encoded).use {
            getAudioInputStream(it.format.toMonoFormat(), it).use(action)
          }
        }
      }
    }
  }

  private fun countFrames(file: Path): Long {
    var length = 0L
    Files.newInputStream(file).use { inputStream ->
      withMonoPcmStream(inputStream) { mono ->
        var x = 0
        val buffer = ByteArray(1 shl 14)
        while (x != -1) {
          x = mono.read(buffer)
          if (x != -1) {
            length += x
          }
        }
      }
    }
    return length / 2
  }

  // Pre-calculate size of WAV file in frames and then use it in this function
  private fun createSizedAudioStream(source: AudioInputStream, size: Long): AudioInputStream {
    if (source.frameLength > 0) return source
    return object : AudioInputStream(ByteArrayInputStream(ByteArray(0)), source.format.toPcmPreservingChannels(), size) {
      override fun skip(n: Long): Long {
        return source.skip(n)
      }

      override fun getFrameLength(): Long {
        return size
      }

      override fun available(): Int {
        return source.available()
      }

      override fun reset() {
        source.reset()
      }

      override fun close() {
        source.close()
      }

      override fun mark(readlimit: Int) {
        source.mark(readlimit)
      }

      override fun markSupported(): Boolean {
        return source.markSupported()
      }

      override fun read(): Int {
        return source.read()
      }

      override fun read(b: ByteArray?): Int {
        return source.read(b)
      }

      override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return source.read(b, off, len)
      }

      override fun getFormat(): AudioFormat {
        return source.format
      }
    }
  }
}