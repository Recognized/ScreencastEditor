package vladsaif.syncedit.plugin.recognition.recognizers

import kotlinx.coroutines.experimental.CancellationException
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.LibrariesLoader
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.recognition.GCredentialProvider
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import vladsaif.syncedit.plugin.sound.SoundProvider
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Supplier

class GSpeechKit : SpeechRecognizer {

  override val name: String
    get() = "Google Speech Kit"

  @Suppress("unchecked_cast")
  @Throws(IOException::class)
  override fun recognize(supplier: Supplier<InputStream>): CompletableFuture<TranscriptData> {
    val speechKitClass = LibrariesLoader.getGSpeechKit()
    val instance = LibrariesLoader.createGSpeechKitInstance(GCredentialProvider.Instance.gSettings!!)
    val method = speechKitClass.getMethod("recognize", InputStream::class.java)
    try {
      // Google mostly accept PCM encoded audio
      // But we can decode it to PCM, if it is in other encoding
      return SoundProvider.withMonoWavFileStream(supplier) { stream ->
        val result = method.invoke(instance, stream) as CompletableFuture<List<List<Any>>>
        result.thenApply(this::parseResponse)
      }

    } catch (ex: InvocationTargetException) {
      if (ex.targetException is ExecutionException || ex.targetException is InterruptedException) {
        throw CancellationException()
      }
      throw ex.targetException
    }
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
}