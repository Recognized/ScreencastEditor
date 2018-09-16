package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import vladsaif.syncedit.plugin.ExEDT
import vladsaif.syncedit.plugin.SoundProvider
import vladsaif.syncedit.plugin.audioview.toolbar.AudioToolWindowManager
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

class OpenAudioAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    FileChooser.chooseFile(descriptor, project, project.projectFile) {
      ACTION_IN_PROGRESS = true
      launch {
        openAudio(project, it)
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !ACTION_IN_PROGRESS
  }

  companion object {
    @Volatile
    private var ACTION_IN_PROGRESS = false

    suspend fun openAudio(project: Project, file: VirtualFile): WaveformModel? {
      ACTION_IN_PROGRESS = true
      return try {
        file.inputStream.use { SoundProvider.getAudioFileFormat(it) }
        AudioToolWindowManager.openAudioFile(project, file)
      } catch (ex: UnsupportedAudioFileException) {
        withContext(ExEDT) {
          errorUnsupportedAudioFile(project, file)
        }
        null
      } catch (ex: IOException) {
        withContext(ExEDT) {
          errorIO(project, ex)
        }
        null
      } finally {
        ACTION_IN_PROGRESS = false
      }
    }
  }
}

