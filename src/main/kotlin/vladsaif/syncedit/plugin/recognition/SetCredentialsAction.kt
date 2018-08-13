package vladsaif.syncedit.plugin.recognition

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException

class SetCredentialsAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    descriptor.title = "Choose file with credentials"
    descriptor.description = "Choose file with credentials that are used in cloud recognition service."
    FileChooser.chooseFile(descriptor, e.project, e.project?.projectFile) { file: VirtualFile ->
      ApplicationManager.getApplication().executeOnPooledThread {
        try {
          CredentialProvider.Instance.setGCredentialsFile(File(file.path).toPath())
          ApplicationManager.getApplication().invokeLater {
            Notification(
                "Screencast Editor",
                "Credentials",
                "Credentials are successfully installed: \"${file.path}\"",
                NotificationType.INFORMATION
            ).notify(e.project)
          }
        } catch (ex: IOException) {
          ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                e.project,
                "Not valid credentials file: \"${file.path}\"",
                "Corrupted credentials"
            )
          }
        }
      }
    }
  }
}
