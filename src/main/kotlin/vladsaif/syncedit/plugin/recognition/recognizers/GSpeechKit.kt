package vladsaif.syncedit.plugin.recognition.recognizers

import com.google.api.core.ApiFutureToListenableFuture
import com.google.cloud.speech.v1p1beta1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.experimental.guava.await
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.recognition.CredentialProvider
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException
import java.io.InputStream

/**
 * @constructor
 * @throws java.io.IOException threw by constructor of [SpeechClient]
 */
class GSpeechKit
@Throws(IOException::class)
private constructor(settings: SpeechSettings) : SpeechClient(settings), SpeechRecognizer {

  @Throws(IOException::class)
  override suspend fun recognize(inputStream: InputStream): TranscriptData {
    val audioBytes = ByteString.readFrom(inputStream)
    val config = RecognitionConfig.newBuilder()
        .setEnableWordTimeOffsets(true)
        .setModel("video")
        .setLanguageCode("en-US")
        .setEnableAutomaticPunctuation(true)
        .build()
    val audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build()
    val response = ApiFutureToListenableFuture(longRunningRecognizeAsync(config, audio)).await()
    val wordData = response.getResults(0)
        .getAlternatives(0)
        .wordsList
        .map { info -> WordData(info.word, getMsRange(info), WordData.State.PRESENTED) }
    return TranscriptData(wordData)
  }

  private fun getMsRange(info: WordInfo): ClosedIntRange {
    return ClosedIntRange((info.startTime.seconds * 1000 + info.startTime.nanos / 1_000_000).toInt(),
        (info.endTime.seconds * 1000 + info.endTime.nanos / 1_000_000).toInt())
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