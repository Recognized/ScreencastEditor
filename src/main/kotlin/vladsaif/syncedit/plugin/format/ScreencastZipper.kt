package vladsaif.syncedit.plugin.format

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.kotlin.idea.debugger.readAction
import org.jetbrains.kotlin.konan.file.use
import vladsaif.syncedit.plugin.model.Screencast
import vladsaif.syncedit.plugin.model.TranscriptData
import vladsaif.syncedit.plugin.sound.EditionsModel
import vladsaif.syncedit.plugin.sound.EditionsView
import vladsaif.syncedit.plugin.util.transferTo
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

object ScreencastZipper {
  private val LOG = logger<ScreencastZipper>()

  fun zipLight(screencast: Screencast, out: Path) {
    ZipperScope(out).use {
      runInEdt {
        it.setSettings(getSettings(screencast))
      }
      screencast.pluginAudio?.let { audio ->
        it.addPluginAudio(audio.audioInputStream)
      }
    }
  }

  fun getSettings(screencast: Screencast): Settings {
    with(screencast) {
      return Settings(
        importedAudioPath = importedAudioPath,
        importedAudioOffset = importedAudio?.model?.offsetFrames ?: 0L,
        importedEditionsView = importedAudio?.editionsModel,
        importedTranscriptData = importedAudio?.data,
        pluginAudioOffset = pluginAudio?.model?.offsetFrames ?: 0,
        pluginEditionsView = pluginAudio?.editionsModel,
        pluginTranscriptData = pluginAudio?.data,
        script = readAction { screencast.scriptDocument?.text } ?: ""
      )
    }
  }

  fun createZip(out: Path, action: ZipperScope.() -> Unit) {
    ZipperScope(out).use(action)
  }

  @Suppress("unused")
  class ZipperScope(val out: Path) : AutoCloseable {
    private val myZipStream = ZipOutputStream(Files.newOutputStream(out).buffered())
    private val myEntrySet = mutableSetOf<EntryType>()
    private var mySettings = Settings()

    init {
      myZipStream.setLevel(0)
      myZipStream.setMethod(ZipOutputStream.DEFLATED)
    }

    fun setSettings(settings: Settings) {
      mySettings = settings
    }

    fun usePluginAudioOutputStream(name: String? = null, block: (OutputStream) -> Unit) {
      if (!myEntrySet.add(EntryType.PLUGIN_AUDIO)) {
        throw IllegalStateException("Audio is already zipped.")
      }
      val zipEntry = ZipEntry(name ?: "audio")
      zipEntry.comment = EntryType.PLUGIN_AUDIO.name
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

    fun addPluginAudio(inputStream: InputStream) {
      usePluginAudioOutputStream { output ->
        inputStream.buffered().use { input ->
          input.transferTo(output)
        }
      }
    }

    fun addImportedAudio(path: Path?) {
      mySettings = mySettings.copy(importedAudioPath = path)
    }

//    fun addPluginAudio(
//      inputStream: Supplier<InputStream>,
//      editionModel: EditionsModel,
//      progressUpdater: (Double) -> Unit = {}
//    ) {
//      var totalLength = 0L
//      var frameSize = 0
//      SoundProvider.withSizedPcmStream(inputStream) {
//        totalLength = it.frameLength
//        frameSize = it.format.frameSize
//      }
//      usePluginAudioOutputStream { out ->
//        SoundProvider.withMonoWavFileStream(inputStream) { wavData ->
//          putWavHeader(out, wavData)
//          editAndSave(wavData, out, editionModel, frameSize, totalLength, progressUpdater)
//        }
//      }
//    }
//
//    /**
//     * Put first 44 bytes of [inputStream] to [outputStream]
//     */
//    private fun putWavHeader(out: OutputStream, inputStream: InputStream) {
//      val header = ByteArray(44)
//      var totalRead = 0
//      while (totalRead != header.size) {
//        totalRead += inputStream.read(header, totalRead, header.size - totalRead)
//      }
//      out.write(header)
//    }
//
//    private fun editAndSave(
//      audio: InputStream,
//      out: OutputStream,
//      editionModel: EditionsModel,
//      frameSize: Int,
//      totalLength: Long,
//      progressUpdater: (Double) -> Unit
//    ) {
//      val buffered = out.buffered()
//      val editions = editionModel.editionsModel
//      val buffer = ByteArray(1 shl 14)
//      var totalFrames = 0L
//      outer@ for (edition in editions) {
//        var needBytes = edition.first.length * frameSize
//        when (edition.second) {
//          EditionsModel.EditionType.CUT -> {
//            totalFrames += needBytes / frameSize
//            progressUpdater(totalFrames.toDouble() / totalLength)
//            while (needBytes != 0L) {
//              val skipped = audio.skip(needBytes)
//              needBytes -= skipped
//              if (skipped == 0L) {
//                break@outer
//              }
//            }
//          }
//          EditionsModel.EditionType.MUTE -> {
//            buffer.fill(0)
//            var needSkip = needBytes
//            while (needBytes != 0L || needSkip != 0L) {
//              if (needBytes != 0L) {
//                val zeroesCount = min(buffer.size.toLong(), needBytes).toInt().modFloor(frameSize)
//                progressUpdater(totalFrames.toDouble() / totalLength)
//                buffered.write(buffer, 0, zeroesCount)
//                totalFrames += zeroesCount / frameSize
//                needBytes -= zeroesCount
//              }
//              if (needSkip != 0L) {
//                val skipped = audio.skip(needSkip)
//                needSkip -= skipped
//                if (skipped == 0L) {
//                  break@outer
//                }
//              }
//            }
//          }
//          EditionsModel.EditionType.NO_CHANGES -> {
//            while (needBytes != 0L) {
//              val read = audio.read(buffer, 0, min(buffer.size.toLong(), needBytes).toInt())
//              if (read == -1) {
//                break@outer
//              }
//              needBytes -= read
//              progressUpdater(totalFrames.toDouble() / totalLength)
//              buffered.write(buffer, 0, read)
//              totalFrames += read / frameSize
//            }
//          }
//        }
//      }
//    }

    fun addScript(script: String) {
      mySettings = mySettings.copy(script = script)
    }

    fun addPluginTranscriptData(data: TranscriptData?) {
      mySettings = mySettings.copy(pluginTranscriptData = data)
    }

    fun addPluginEditionsView(editionsView: EditionsView?) {
      mySettings = mySettings.copy(pluginEditionsView = editionsView)
    }

    fun addImportedTranscriptData(data: TranscriptData?) {
      mySettings = mySettings.copy(importedTranscriptData = data)
    }

    fun addImportedEditionsView(editionsView: EditionsView?) {
      mySettings = mySettings.copy(importedEditionsView = editionsView)
    }

    fun setImportedAudioOffset(offsetFrames: Long) {
      mySettings = mySettings.copy(importedAudioOffset = offsetFrames)
    }

    fun setPluginAudioOffset(offsetFrames: Long) {
      mySettings = mySettings.copy(pluginAudioOffset = offsetFrames)
    }

    private fun writeEntry(name: String, type: EntryType, data: ByteArray) {
      with(myZipStream) {
        val entry = ZipEntry(name)
        entry.comment = type.name
        putNextEntry(entry)
        write(data)
        closeEntry()
      }
    }

    private fun saveSettings() {
      val stream = ByteArrayOutputStream()
      JAXB.marshal(mySettings, stream)
      writeEntry(out.fileName.toString() + ".settings", EntryType.SETTINGS, stream.toByteArray())
    }

    override fun close() {
      try {
        saveSettings()
        myZipStream.close()
      } catch (ex: Exception) {
        LOG.info(ex)
      }
    }

  }

  @XmlAccessorType(XmlAccessType.FIELD)
  data class Settings(
    val pluginAudioOffset: Long = 0,
    @field:XmlJavaTypeAdapter(EditionsViewAdapter::class)
    val pluginEditionsView: EditionsView? = null,
    val pluginTranscriptData: TranscriptData? = null,
    val importedAudioOffset: Long = 0,
    @field:XmlJavaTypeAdapter(PathAdapter::class)
    val importedAudioPath: Path? = null,
    @field:XmlJavaTypeAdapter(EditionsViewAdapter::class)
    val importedEditionsView: EditionsView? = null,
    val importedTranscriptData: TranscriptData? = null,
    val script: String = ""
  )

  private class EditionsViewAdapter : XmlAdapter<String, EditionsView>() {
    override fun marshal(v: EditionsView): String {
      return Base64.getEncoder().encodeToString(v.serialize())
    }

    override fun unmarshal(v: String): EditionsView {
      return EditionsModel.deserialize(Base64.getDecoder().decode(v))
    }
  }

  private class PathAdapter : XmlAdapter<String, Path>() {
    override fun marshal(v: Path): String {
      return v.toAbsolutePath().toString()
    }

    override fun unmarshal(v: String): Path {
      return Paths.get(v)
    }
  }

  enum class EntryType {
    PLUGIN_AUDIO,
    SETTINGS
  }
}