package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.recognition.GCredentialProvider
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
          GCredentialProvider.Instance.setGCredentialsFile(File(file.path).toPath())
          ApplicationManager.getApplication().invokeLater {
            infoCredentialsOK(e.project, file)
          }
        } catch (ex: IOException) {
          ApplicationManager.getApplication().invokeLater {
            errorCredentials(e.project, file)
          }
        }
      }
    }
  }
}
