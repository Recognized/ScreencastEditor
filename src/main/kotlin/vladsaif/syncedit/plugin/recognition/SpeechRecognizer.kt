package vladsaif.syncedit.plugin.recognition

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import vladsaif.syncedit.plugin.LibrariesLoader
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.actions.errorRequirementsNotSatisfied
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType
import java.io.File
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
    private val LOG = logger<SpeechRecognizer>()

    val EP_NAME = ExtensionPointName.create<SpeechRecognizer>("vladsaif.syncedit.plugin.recognition.SpeechRecognizer")

    @Volatile
    var RECOGNITION_IN_PROGRESS = false
      private set

    fun checkRequirements(project: Project): Boolean {
      try {
        ChooseRecognizerAction.CURRENT_RECOGNIZER.checkRequirements()
        return true
      } catch (ex: IOException) {
        errorRequirementsNotSatisfied(project, ex)
      }
      return false
    }

    fun runRecognitionTask(project: Project, model: MultimediaModel, audio: VirtualFile) {
      RECOGNITION_IN_PROGRESS = true
      val recognizeTask = RecognizeTask(
          project,
          "Getting transcript for $audio",
          File(audio.path).toPath(),
          model
      )
      ProgressManager.getInstance().run(recognizeTask)
    }
  }

  private class RecognizeTask(
      project: Project,
      title: String,
      private val path: Path,
      private val multimedia: MultimediaModel
  ) : Task.Backgroundable(project, title, true) {

    private var myFuture: CompletableFuture<TranscriptData>? = null

    override fun run(indicator: ProgressIndicator) {
      indicator.isIndeterminate = true
      try {
        val data = try {
          indicator.checkCanceled()
          val future = ChooseRecognizerAction.CURRENT_RECOGNIZER.recognize(path)
          myFuture = future
          indicator.checkCanceled()
          runBlocking {
            future.await()
          }
        } finally {
          // Probably, library won't be used again after recognition
          LibrariesLoader.releaseClassloader()
        }
        indicator.checkCanceled()
        ApplicationManager.getApplication().invokeAndWait {
          ApplicationManager.getApplication().runWriteAction {
            val xml = PsiFileFactory.getInstance(project).createFileFromText(
                "${FileUtil.getNameWithoutExtension(path.toFile())}.${InternalFileType.defaultExtension}",
                InternalFileType,
                data.toXml(),
                0L,
                true
            )
            multimedia.setAndReadXml(xml.virtualFile)
            FileEditorManager.getInstance(project).openFile(xml.virtualFile, true)
            indicator.stop()
          }
        }
      } catch (_: ProcessCanceledException) {
        // ignore
      } catch (ex: Throwable) {
        LOG.info(ex)
        val cause = getCause(ex)
        ApplicationManager.getApplication().invokeLater {
          Notification(
              "Screencast Editor",
              "Recognition failed",
              "${if (cause is IOException) "I/O error occurred: " else "Error:"} ${cause.message}",
              NotificationType.ERROR
          ).notify(project)
        }
      } finally {
        RECOGNITION_IN_PROGRESS = false
      }
    }

    private fun getCause(x: Throwable, depth: Int = 0): Throwable {
      return if (x.cause == null || depth > 10) x else getCause(x.cause!!, depth + 1)
    }

    override fun onCancel() {
      myFuture?.cancel(true)
      super.onCancel()
    }
  }
}
