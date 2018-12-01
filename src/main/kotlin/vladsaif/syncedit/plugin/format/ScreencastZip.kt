package vladsaif.syncedit.plugin.format

import vladsaif.syncedit.plugin.format.ScreencastZipper.EntryType.PLUGIN_AUDIO
import vladsaif.syncedit.plugin.format.ScreencastZipper.EntryType.SETTINGS
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.xml.bind.JAXB

class ScreencastZip(val path: Path) {
  val hasPluginAudio: Boolean by lazy { isDataSet(PLUGIN_AUDIO) }
  val audioInputStream: InputStream
    get() = getInputStreamByType(PLUGIN_AUDIO) ?: throw IllegalStateException("Audio is not set")

  fun readSettings(): ScreencastZipper.Settings {
    return getInputStreamByType(SETTINGS)?.use {
      JAXB.unmarshal(it, ScreencastZipper.Settings::class.java)
    } ?: ScreencastZipper.Settings()
  }

  private fun isDataSet(type: ScreencastZipper.EntryType): Boolean {
    return ZipFile(path.toFile()).use { file ->
      file.entries().asSequence().any { it.comment == type.name }
    }
  }

  private fun getInputStreamByType(type: ScreencastZipper.EntryType): InputStream? {
    val exists = ZipFile(path.toFile()).use { file ->
      file.entries()
        .asSequence()
        .any { it.comment == type.name }
    }
    return if (exists) ZipEntryInputStream(ZipFile(path.toFile()), type.name) else null
  }

  // Actual zipStream cannot skip properly
  private class ZipEntryInputStream(private val file: ZipFile, comment: String) : InputStream() {

    private val myStream: InputStream = file.getInputStream(file.entries().asSequence().first { it.comment == comment })

    override fun read() = myStream.read()

    override fun read(b: ByteArray?) = myStream.read(b)

    override fun read(b: ByteArray?, off: Int, len: Int) = myStream.read(b, off, len)

    override fun available() = myStream.available()

    override fun reset() = myStream.reset()

    override fun mark(readlimit: Int) = myStream.mark(readlimit)

    override fun markSupported() = myStream.markSupported()

    override fun skip(n: Long) = myStream.skip(n)

    override fun close() {
      myStream.close()
      file.close()
    }
  }
}