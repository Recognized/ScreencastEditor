package vladsaif.syncedit.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.SoundProvider
import javax.sound.sampled.UnsupportedAudioFileException

class BindAudioWithScript : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project ?: return
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
          val modelScript = MultimediaModel.get(script)
          val modelAudio = MultimediaModel.get(audio)
          when {
            modelAudio != null && modelScript != null && modelAudio != modelScript -> {
              Messages.showErrorDialog(
                  project,
                  "Cannot bind already bound files.",
                  "Bind error"
              )
            }

            modelAudio != null && modelScript != null -> {
              Messages.showInfoMessage(
                  project,
                  "Audio: \"${audio.path}\" \n" +
                      "Script: \"${script.path}\" \n" +
                      "are already bound.",
                  "Bind audio and script"
              )
            }

            else -> {
              // Setting audio file is pretty time consuming operation
              ApplicationManager.getApplication().executeOnPooledThread {
                val newModel = modelAudio ?: modelScript ?: MultimediaModel(project)
                newModel.audioFile = audio
                newModel.scriptFile = script
                ApplicationManager.getApplication().invokeLater {
                  Notification(
                      "Screencast Editor",
                      "Audio and script are bound",
                      "Successfully bound\n" +
                          "Audio: \"${audio.path}\" \n" +
                          "Script: \"${script.path}\" \n",
                      NotificationType.INFORMATION
                  ).notify(e.project)
                }
              }
            }
          }

        }
      } catch (ex: UnsupportedAudioFileException) {
        Messages.showErrorDialog(
            project,
            "Audio file format is not supported. File: $audio",
            "Unsupported file format"
        )
      }
    }
  }
}