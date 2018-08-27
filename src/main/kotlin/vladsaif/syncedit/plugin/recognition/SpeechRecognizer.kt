package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.extensions.ExtensionPointName
import vladsaif.syncedit.plugin.TranscriptData
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface SpeechRecognizer {

  /**
   * @return Recognized [TranscriptData] in audio from [file].
   * @throws java.io.IOException If I/O error occurred
   * @throws Throwable if input stream contains trash
   */
  @Throws(IOException::class)
  fun recognize(file: Path): CompletableFuture<TranscriptData>

  /**
   * Check if supplied [inputStream] can be recognized.
   * @throws IOException if [inputStream] cannot be recognized for some reason
   * which is described in exception message.
   */
  @Throws(IOException::class)
  fun checkRequirements()

  val name: String

  companion object {
    val EP_NAME = ExtensionPointName.create<SpeechRecognizer>("vladsaif.syncedit.plugin.recognition.SpeechRecognizer")

    @Throws(IOException::class)
    fun getRecognizer(): SpeechRecognizer {
      return ChooseRecognizerAction.currentRecognizer
    }
  }
}
