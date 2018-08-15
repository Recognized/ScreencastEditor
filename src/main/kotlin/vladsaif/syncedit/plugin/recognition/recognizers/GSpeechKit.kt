package vladsaif.syncedit.plugin.recognition.recognizers

import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.LibrariesLoader
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.recognition.CredentialProvider
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * @constructor
 * @throws java.io.IOException threw by constructor of [SpeechClient]
 */
class GSpeechKit
@Throws(IOException::class)
private constructor(path: Path) : SpeechRecognizer {
  private val myInternalKit = LibrariesLoader.createGSpeechKitInstance(path)

  @Suppress("unchecked_cast")
  @Throws(IOException::class)
  override suspend fun recognize(inputStream: InputStream): TranscriptData {
    val speechKitClass = LibrariesLoader.getGSpeechKit()
    val method = speechKitClass.getMethod("recognize", InputStream::class.java)
    val result = try {
      method.invoke(myInternalKit, inputStream) as List<List<Any>>
    } catch (ex: InvocationTargetException) {
      if (ex.targetException !is ExecutionException && ex.targetException !is InterruptedException) {
        throw CancellationException()
      }
      throw ex.targetException
    }
    return parseResponse(result)
  }

  private fun parseResponse(result: List<List<Any>>): TranscriptData {
    val list = mutableListOf<WordData>()
    for (x in result) {
      val word = x[0] as String
      val startTime = x[1] as Int
      val endTime = x[2] as Int
      list.add(WordData(word, IRange(startTime, endTime), WordData.State.PRESENTED, -1))
    }
    return TranscriptData(list)
  }

  companion object {

    @Throws(IOException::class, IllegalStateException::class)
    fun create(): GSpeechKit {
      val settings = CredentialProvider.Instance.gSettings
          ?: throw IllegalStateException("Set credentials before creating GSpeechKit")
      return GSpeechKit(settings)
    }
  }
}