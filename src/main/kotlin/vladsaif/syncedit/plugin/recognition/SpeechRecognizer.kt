package vladsaif.syncedit.plugin.recognition

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.future.await
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.model.TranscriptData
import vladsaif.syncedit.plugin.recognition.recognizers.GSpeechKit
import vladsaif.syncedit.plugin.util.ExEDT
import vladsaif.syncedit.plugin.util.LibrariesLoader
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

interface SpeechRecognizer {

  /**
   * @return Recognized [TranscriptData] in audio from [supplier].
   * @throws java.io.IOException If I/O error occurred.
   */
  @Throws(IOException::class)
  fun recognize(supplier: Supplier<InputStream>): CompletableFuture<TranscriptData>

  @Throws(IOException::class)
  fun recognize(path: Path): CompletableFuture<TranscriptData> {
    return recognize(Supplier { Files.newInputStream(path) })
  }

  /**
   * Check if current recognizer is ready to work.
   * @throws IOException If recognition cannot be performed for some reason
   * which is described in exception message.
   */
  @Throws(IOException::class)
  fun checkRequirements()

  val name: String

  companion object {
    private val LOG = logger<SpeechRecognizer>()
    private var CURRENT_RECOGNIZER: SpeechRecognizer = GSpeechKit()
    val EP_NAME = ExtensionPointName.create<SpeechRecognizer>("vladsaif.syncedit.plugin.recognition.SpeechRecognizer")

    fun getCurrentRecognizer(): SpeechRecognizer = CURRENT_RECOGNIZER

    fun setCurrentRecognizer(recognizer: SpeechRecognizer) {
      CURRENT_RECOGNIZER = recognizer
    }

    fun runRecognitionTask(model: ScreencastFile) = TASK_RUNNER.offer(model)

    private val TASK_RUNNER = GlobalScope.actor<ScreencastFile> {
      for (file in channel) {
        val task = RecognizeTask(file, launch { runRecognition(file) })
        ProgressManager.getInstance().run(task)
        try {
          task.job.join()
        } catch (_: CancellationException) {
        }
      }
    }

    private suspend fun runRecognition(file: ScreencastFile) {
      try {
        val data = withContext(Dispatchers.Default) {
          try {
            SpeechRecognizer.getCurrentRecognizer().recognize(Supplier { file.audioInputStream }).await()
          } finally {
            // Probably, library won't be used again after recognition
            LibrariesLoader.releaseClassloader()
          }
        }
        withContext(ExEDT) {
          ApplicationManager.getApplication().runWriteAction {
            file.loadTranscriptData(data)
          }
        }
      } catch (ex: Throwable) {
        if (ex is CancellationException) {
          throw ex
        }
        LOG.info(ex)
        val cause = getCause(ex)
        withContext(ExEDT) {
          Notification(
              "Screencast Editor",
              "Recognition failed",
              "${if (cause is IOException) "I/O error occurred: " else "Error:"} ${cause.message}",
              NotificationType.ERROR
          ).notify(file.project)
        }
      }
    }

    private fun getCause(x: Throwable, depth: Int = 0): Throwable {
      return if (x.cause == null || depth > 10) x else getCause(x.cause!!, depth + 1)
    }

  }

  private class RecognizeTask(
      file: ScreencastFile,
      val job: Job
  ) : Task.Backgroundable(file.project, "Getting transcript for ${file.file.fileName}", true) {

    override fun run(indicator: ProgressIndicator) {
      indicator.isIndeterminate = true
      if (!indicator.isRunning) indicator.start()
      try {
        runBlocking { job.join() }
      } catch (_: Throwable) {
      }
      if (indicator.isRunning) indicator.stop()
    }

    override fun onCancel() {
      job.cancel()
    }
  }
}
