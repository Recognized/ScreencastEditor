package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
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
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import vladsaif.syncedit.plugin.LibrariesLoader
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType
import vladsaif.syncedit.plugin.recognition.ChooseRecognizerAction
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class RecognizeAudioAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    if (!checkRequirements(project)) return
    descriptor.title = "Choose audio file"
    descriptor.description = "Choose audio file for cloud recognition"
    FileChooser.chooseFile(descriptor, e.project, e.project?.projectFile) { file: VirtualFile ->
      ACTION_IN_PROGRESS = true
      launch {
        val waveform = OpenAudioAction.openAudio(project, file) ?: return@launch
        runRecognitionTask(project, waveform.multimediaModel, file)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !ACTION_IN_PROGRESS
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
          val future = SpeechRecognizer.getRecognizer().recognize(path)
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
        ACTION_IN_PROGRESS = false
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

  companion object {
    private val LOG = logger<RecognizeAudioAction>()
    @Volatile
    private var ACTION_IN_PROGRESS = false

    fun checkRequirements(project: Project): Boolean {
      try {
        ChooseRecognizerAction.currentRecognizer.checkRequirements()
        return true
      } catch (ex: IOException) {
        errorRequirementsNotSatisfied(project, ex)
      }
      return false
    }

    fun runRecognitionTask(project: Project, model: MultimediaModel, audio: VirtualFile) {
      val recognizeTask = RecognizeTask(
          project,
          "Getting transcript for $audio",
          File(audio.path).toPath(),
          model
      )
      ProgressManager.getInstance().run(recognizeTask)
    }
  }
}
