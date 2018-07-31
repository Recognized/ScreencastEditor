package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
            try {
                CredentialProvider.Instance.setGCredentialsFile(File(file.path).toPath())
            } catch (ex: IOException) {
                Messages.showErrorDialog(e.project, ex.message, "I/O error occurred")
            }
        }
    }
}
