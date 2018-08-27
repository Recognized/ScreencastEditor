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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import vladsaif.syncedit.plugin.LibrariesLoader
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType
import vladsaif.syncedit.plugin.recognition.ChooseRecognizerAction
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.File
import java.io.IOException
import java.nio.file.Path

class RecognizeAudioAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    if (!checkRequirements(project)) return
    descriptor.title = "Choose audio file"
    descriptor.description = "Choose audio file for cloud recognition"
    FileChooser.chooseFile(descriptor, e.project, e.project?.projectFile) { file: VirtualFile ->
      val waveform = OpenAudioAction.openAudio(project, file) ?: return@chooseFile
      runRecognitionTask(project, waveform.multimediaModel, file)
    }
  }

  private class RecognizeTask(
      project: Project,
      title: String,
      private val path: Path,
      private val multimedia: MultimediaModel
  ) : Task.Backgroundable(project, title, true) {

    private var myJob: Job? = null

    override fun run(indicator: ProgressIndicator) {
      indicator.isIndeterminate = true
      try {
        val job = Job()
        myJob = job
        runBlocking(job) {
          val data = SpeechRecognizer.getRecognizer().recognize(path).await()
          // Probably, after recognition, library won't be used again
          LibrariesLoader.releaseClassloader()
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
        }
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
      }
    }

    private fun getCause(x: Throwable, depth: Int = 0): Throwable {
      return if (x.cause == null || depth > 10) x else getCause(x.cause!!, depth + 1)
    }

    override fun onCancel() {
      myJob?.cancel()
      LibrariesLoader.releaseClassloader()
      super.onCancel()
    }
  }

  companion object {
    private val LOG = logger<RecognizeAudioAction>()
    fun checkRequirements(project: Project): Boolean {
      try {
        ChooseRecognizerAction.currentRecognizer.checkRequirements()
        return true
      } catch (ex: IOException) {
        Messages.showWarningDialog(
            project,
            ex.message ?: "Unknown error.",
            "Requirements not satisfied"
        )
      }
      return false
    }

    fun runRecognitionTask(project: Project, model: MultimediaModel, audio: VirtualFile) {
      try {
        val recognizeTask = RecognizeTask(
            project,
            "Getting transcript for $audio",
            File(audio.path).toPath(),
            model
        )
        ProgressManager.getInstance().run(recognizeTask)
      } catch (ex: IOException) {
        Messages.showErrorDialog(project, ex.message, "I/O error occurred")
      }
    }

  }
}
