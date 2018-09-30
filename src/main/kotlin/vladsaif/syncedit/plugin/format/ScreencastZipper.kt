package vladsaif.syncedit.plugin.format

import vladsaif.syncedit.plugin.TranscriptData
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ScreencastZipper {

  fun createZipBuilder(destination: Path) = ZipBuilder(destination)

  class ZipBuilder(val destination: Path) {
    var audio: Path? = null
      private set
    var data: TranscriptData? = null
      private set
    var script: String? = null
      private set

    fun addAudio(path: Path): ZipBuilder {
      audio = path
      return this
    }

    fun addScript(path: Path): ZipBuilder {
      script = Files.newBufferedReader(path).use { reader ->
        reader.lines().collect(Collectors.joining("\n"))
      }
      return this
    }

    fun addScript(value: String): ZipBuilder {
      script = value
      return this
    }

    fun addTranscriptData(data: TranscriptData): ZipBuilder {
      this.data = data
      return this
    }

    fun zip() {
      Files.newOutputStream(destination).use { stream ->
        ZipOutputStream(stream.buffered()).use { zipStream ->
          with(zipStream) {
            setLevel(0)
            if (script != null) {
              val entry = ZipEntry(destination.fileName.toString() + ".kts")
              entry.comment = EntryType.SCRIPT.name
              putNextEntry(entry)
              write(script!!.toByteArray(Charset.forName("UTF-8")))
              closeEntry()
            }
            if (audio != null) {
              val zipEntry = ZipEntry(audio!!.fileName.toString())
              zipEntry.comment = EntryType.AUDIO.name
              putNextEntry(zipEntry)
              Files.copy(audio, this)
              closeEntry()
            }
            if (data != null) {
              val entry = ZipEntry(destination.fileName.toString() + ".transcript")
              entry.comment = EntryType.TRANSCRIPT_DATA.name
              putNextEntry(entry)
              write(data!!.toXml().toByteArray(Charset.forName("UTF-8")))
              closeEntry()
            }
          }
        }
      }
    }
  }

  enum class EntryType {
    AUDIO, TRANSCRIPT_DATA, SCRIPT, BINDINGS
  }
}