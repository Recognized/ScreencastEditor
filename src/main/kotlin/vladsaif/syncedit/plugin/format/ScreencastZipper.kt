package vladsaif.syncedit.plugin.format

import com.intellij.openapi.diagnostic.logger
import vladsaif.syncedit.plugin.model.TranscriptData
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.SoundProvider
import vladsaif.syncedit.plugin.sound.impl.modFloor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

private val LOG = logger<ScreencastZipper>()

class ScreencastZipper(val destination: Path) : AutoCloseable {
  private val myZipStream = ZipOutputStream(Files.newOutputStream(destination).buffered())
  private val myEntrySet = mutableSetOf<EntryType>()

  init {
    myZipStream.setLevel(0)
    myZipStream.setMethod(ZipOutputStream.DEFLATED)
  }

  @Synchronized
  fun useAudioOutputStream(name: String? = null, block: (OutputStream) -> Unit) {
    if (!myEntrySet.add(EntryType.AUDIO)) {
      throw IllegalStateException("Audio is already zipped.")
    }
    val zipEntry = ZipEntry(name ?: "audio")
    zipEntry.comment = EntryType.AUDIO.name
    myZipStream.putNextEntry(zipEntry)
    val outputStream = object : OutputStream() {
      override fun write(b: Int) {
        myZipStream.write(b)
      }

      override fun write(b: ByteArray?) {
        myZipStream.write(b)
      }

      override fun write(b: ByteArray?, off: Int, len: Int) {
        myZipStream.write(b, off, len)
      }

      override fun close() {
        myZipStream.closeEntry()
      }
    }
    outputStream.buffered().use(block)
  }

  @Synchronized
  fun addAudio(
      audio: Path
  ) {
    addAudio(Files.newInputStream(audio))
  }

  @Synchronized
  fun addAudio(
      inputStream: InputStream
  ) {
    useAudioOutputStream { output ->
      inputStream.buffered().use { input ->
        input.transferTo(output)
      }
    }
  }

  @Synchronized
  fun addAudio(
      audio: Path,
      editionModel: EditionModel,
      progressUpdater: (Double) -> Unit = {}
  ) {
    addAudio(Supplier { Files.newInputStream(audio) }, editionModel, progressUpdater)
  }

  @Synchronized
  fun addAudio(
      inputStream: Supplier<InputStream>,
      editionModel: EditionModel,
      progressUpdater: (Double) -> Unit = {}
  ) {
    var totalLength = 0L
    var frameSize = 0
    SoundProvider.withSizedPcmStream(inputStream) {
      totalLength = it.frameLength
      frameSize = it.format.frameSize
    }
    useAudioOutputStream { out ->
      SoundProvider.withMonoWavFileStream(inputStream) { wavData ->
        putWavHeader(out, wavData)
        editAndSave(wavData, out, editionModel, frameSize, totalLength, progressUpdater)
      }
    }
  }

  /**
   * Put first 44 bytes of [inputStream] to [outputStream]
   */
  private fun putWavHeader(out: OutputStream, inputStream: InputStream) {
    val header = ByteArray(44)
    var totalRead = 0
    while (totalRead != header.size) {
      totalRead += inputStream.read(header, totalRead, header.size - totalRead)
    }
    out.write(header)
  }

  @Synchronized
  private fun editAndSave(
      audio: InputStream,
      out: OutputStream,
      editionModel: EditionModel,
      frameSize: Int,
      totalLength: Long,
      progressUpdater: (Double) -> Unit
  ) {
    val buffered = out.buffered()
    val editions = editionModel.editions
    val buffer = ByteArray(1 shl 14)
    var totalFrames = 0L
    outer@ for (edition in editions) {
      var needBytes = edition.first.length * frameSize
      when (edition.second) {
        EditionModel.EditionType.CUT -> {
          totalFrames += needBytes / frameSize
          progressUpdater(totalFrames.toDouble() / totalLength)
          while (needBytes != 0L) {
            val skipped = audio.skip(needBytes)
            needBytes -= skipped
            if (skipped == 0L) {
              break@outer
            }
          }
        }
        EditionModel.EditionType.MUTE -> {
          buffer.fill(0)
          var needSkip = needBytes
          while (needBytes != 0L || needSkip != 0L) {
            if (needBytes != 0L) {
              val zeroesCount = min(buffer.size.toLong(), needBytes)
                  .toInt()
                  .modFloor(frameSize)
              progressUpdater(totalFrames.toDouble() / totalLength)
              buffered.write(buffer, 0, zeroesCount)
              totalFrames += zeroesCount / frameSize
              needBytes -= zeroesCount
            }
            if (needSkip != 0L) {
              val skipped = audio.skip(needSkip)
              needSkip -= skipped
              if (skipped == 0L) {
                break@outer
              }
            }
          }
        }
        EditionModel.EditionType.NO_CHANGES -> {
          while (needBytes != 0L) {
            val read = audio.read(buffer, 0, min(buffer.size.toLong(), needBytes).toInt())
            if (read == -1) {
              break@outer
            }
            needBytes -= read
            progressUpdater(totalFrames.toDouble() / totalLength)
            buffered.write(buffer, 0, read)
            totalFrames += read / frameSize
          }
        }
      }
    }
  }

  @Synchronized
  fun addScript(script: String): ScreencastZipper {
    if (!myEntrySet.add(EntryType.SCRIPT)) {
      throw IllegalStateException("Script is already zipped.")
    }
    with(myZipStream) {
      val entry = ZipEntry(destination.fileName.toString() + ".kts")
      entry.comment = EntryType.SCRIPT.name
      putNextEntry(entry)
      write(script.toByteArray(Charset.forName("UTF-8")))
      closeEntry()
    }
    return this
  }

  @Synchronized
  fun addTranscriptData(data: TranscriptData): ScreencastZipper {
    if (!myEntrySet.add(EntryType.TRANSCRIPT_DATA)) {
      throw IllegalStateException("Transcript is already zipped.")
    }
    with(myZipStream) {
      val entry = ZipEntry(destination.fileName.toString() + ".transcript")
      entry.comment = EntryType.TRANSCRIPT_DATA.name
      putNextEntry(entry)
      write(data.toXml().toByteArray(Charset.forName("UTF-8")))
      closeEntry()
    }
    return this
  }

  fun addEditionModel(editionModel: EditionModel): ScreencastZipper {
    if (!myEntrySet.add(EntryType.EDITION_MODEL)) {
      throw IllegalStateException("Edition model is already zipped.")
    }
    with(myZipStream) {
      val entry = ZipEntry(destination.fileName.toString() + "_edition_model")
      entry.comment = EntryType.EDITION_MODEL.name
      putNextEntry(entry)
      write(editionModel.serialize())
      closeEntry()
    }
    return this
  }

  override fun close() {
    try {
      myZipStream.close()
    } catch (ex: Exception) {
      LOG.info(ex)
    }
  }


  enum class EntryType {
    AUDIO, TRANSCRIPT_DATA, SCRIPT, BINDINGS, EDITION_MODEL
  }
}

// Exact implementation from Java 9 JDK
@Throws(IOException::class)
fun InputStream.transferTo(out: OutputStream): Long {
  var transferred: Long = 0
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  var read: Int
  while (true) {
    read = this.read(buffer, 0, DEFAULT_BUFFER_SIZE)
    println(read)
    if (read < 0) break
    out.write(buffer, 0, read)
    transferred += read.toLong()
  }
  return transferred
}