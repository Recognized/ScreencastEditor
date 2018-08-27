package vladsaif.syncedit.plugin.recognition.recognizers

import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.experimental.CancellationException
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.recognition.GCredentialProvider
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread

class GSpeechKit : SpeechRecognizer {

  override val name: String
    get() = "Google speech kit"

  @Suppress("unchecked_cast")
  @Throws(IOException::class)
  override fun recognize(file: Path): CompletableFuture<TranscriptData> {
    val speechKitClass = LibrariesLoader.getGSpeechKit()
    val instance = LibrariesLoader.createGSpeechKitInstance(GCredentialProvider.Instance.gSettings!!)
    val method = speechKitClass.getMethod("recognize", InputStream::class.java)
    try {
      // Google mostly accept PCM encoded audio
      Files.newInputStream(file).use {
        val buffered = it.buffered()
        val format = SoundProvider.getAudioFileFormat(buffered).format
        if (format == MONO_FORMAT) {
          return (method.invoke(instance, buffered) as CompletableFuture<List<List<Any>>>)
              .thenApply(this::parseResponse)
        }
      }
      // But we can decode it to PCM, if it is in other encoding
      val (stream, lock) = convertAudioLazy(file)
      LOG.info("Audio converted: $file")
      try {
        val result = method.invoke(instance, stream) as CompletableFuture<List<List<Any>>>
        return result.thenApply(this::parseResponse)
      } finally {
        lock.unlock()
        LOG.info("Audio sent: $file")
      }
    } catch (ex: InvocationTargetException) {
      if (ex.targetException is ExecutionException || ex.targetException is InterruptedException) {
        throw CancellationException()
      }
      throw ex.targetException
    }
  }

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
        SoundProvider.getAudioInputStream(source.buffered()).use { encoded ->
          SoundProvider.getAudioInputStream(encoded.format.toRecognitionFormat(), encoded).use { decoded ->
            SoundProvider.getAudioInputStream(MONO_FORMAT, decoded).use { mono ->
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
      }
    }
    return BufferedInputStream(pipeIn) to keepAlive
  }

  // Pre-calculate size of WAV file in frames and then use it in this function
  private fun createSizedAudioStream(source: AudioInputStream, size: Long): AudioInputStream {
    return object : AudioInputStream(ByteArrayInputStream(ByteArray(0)), source.format.toRecognitionFormat(), size) {
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

  private fun countFrames(file: Path): Long {
    var length = 0L
    Files.newInputStream(file).use { inputStream ->
      SoundProvider.getAudioInputStream(inputStream.buffered()).use { encoded ->
        SoundProvider.getAudioInputStream(encoded.format.toRecognitionFormat(), encoded).use { decoded ->
          SoundProvider.getAudioInputStream(MONO_FORMAT, decoded).use { mono ->
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
      }
    }
    LOG.info("Length: ${length / 2}")
    return length / 2
  }

  private fun parseResponse(result: List<List<Any>>): TranscriptData {
    val list = mutableListOf<WordData>()
    for (x in result) {
      val word = x[0] as String
      val startTime = x[1] as Int
      val endTime = x[2] as Int
      list.add(WordData(word, IRange(startTime, endTime)))
    }
    return TranscriptData(list)
  }

  @Throws(IOException::class)
  override fun checkRequirements() {
    if (GCredentialProvider.Instance.gSettings == null) {
      throw IOException("Credentials for cloud service account should be set before recognition is used.")
    }
  }

  companion object {
    private val LOG = logger<GSpeechKit>()
    private fun AudioFormat.toRecognitionFormat() =
        AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            44100f,
            16,
            channels,
            2 * channels,
            44100f,
            false)

    private val MONO_FORMAT =
        AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            44100f,
            16,
            1,
            2,
            44100f,
            false)
  }
}