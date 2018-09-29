package vladsaif.syncedit.plugin.actions.tools

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.actions.errorNoModelForFile
import vladsaif.syncedit.plugin.diffview.DiffDialogFactory

class ManageScriptTranscriptRelations : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    FileChooser.chooseFile(descriptor, project, null) {
      val model = ScreencastFile.get(it)
      if (model == null) {
        errorNoModelForFile(project, it)
      } else {
        DiffDialogFactory.showWindow(model)
      }
    }
  }
}