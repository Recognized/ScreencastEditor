package vladsaif.syncedit.plugin.actions.tools

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.SoundProvider
import vladsaif.syncedit.plugin.actions.errorAlreadyBoundToDifferent
import vladsaif.syncedit.plugin.actions.errorUnsupportedAudioFile
import vladsaif.syncedit.plugin.actions.infoAlreadyBound
import vladsaif.syncedit.plugin.actions.notifySuccessfullyBound
import javax.sound.sampled.UnsupportedAudioFileException

class BindAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    descriptor.description = "Choose audio file"
    descriptor.title = "Bind audio and script"
    FileChooser.chooseFile(descriptor, project, project.baseDir) { audio ->
      try {
        audio.inputStream.use { SoundProvider.getAudioFileFormat(it) }
        val ktDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("kts")
        ktDescriptor.title = descriptor.title
        ktDescriptor.description = "Choose script file"
        FileChooser.chooseFile(ktDescriptor, project, audio) { script ->
          bind(project, audio, script)
        }
      } catch (ex: UnsupportedAudioFileException) {
        errorUnsupportedAudioFile(project, audio)
      }
    }
  }

  companion object {

    fun bind(project: Project, audio: VirtualFile, script: VirtualFile) {
      val modelScript = MultimediaModel.get(script)
      val modelAudio = MultimediaModel.get(audio)
      when {
        modelAudio != null && modelScript != null && modelAudio != modelScript -> {
          errorAlreadyBoundToDifferent(project)
        }
        modelAudio != null && modelScript != null -> {
          infoAlreadyBound(project, script, audio)
        }

        else -> {
          // Setting audio file is pretty time consuming operation
          ApplicationManager.getApplication().executeOnPooledThread {
            val newModel = modelAudio ?: modelScript ?: MultimediaModel(project)
            newModel.audioFile = audio
            newModel.scriptFile = script
            ApplicationManager.getApplication().invokeLater {
              notifySuccessfullyBound(project, script, audio)
            }
          }
        }
      }
    }
  }
}