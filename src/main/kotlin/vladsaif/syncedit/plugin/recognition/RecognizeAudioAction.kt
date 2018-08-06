package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
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
import kotlinx.coroutines.experimental.runBlocking
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.audioview.toolbar.OpenAudioAction
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.experimental.coroutineContext

class RecognizeAudioAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    if (CredentialProvider.Instance.gSettings == null) {
      showNoCredentialsDialog(project)
      return
    }
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
      runBlocking {
        myJob = coroutineContext[Job]
        val data = Files.newInputStream(path).use {
          SpeechRecognizer.getDefault().recognize(it)
        }
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
    }

    override fun onCancel() {
      myJob?.cancel()
      super.onCancel()
    }
  }

  companion object {
    fun showNoCredentialsDialog(project: Project) {
      Messages.showWarningDialog(
          project,
          "Credentials for cloud service account should be set before recognition is used",
          "Credentials not found"
      )
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
