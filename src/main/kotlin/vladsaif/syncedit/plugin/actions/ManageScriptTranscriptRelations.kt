package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.diffview.DiffDialogFactory

class ManageScriptTranscriptRelations : AnAction() {

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    FileChooser.chooseFile(descriptor, project, project.baseDir) {
      val model = MultimediaModel.get(it)
      when {
        model == null -> {
          errorNoModelForFile(project, it)
        }
        it == model.transcriptFile && model.scriptFile == null -> {
          errorNoScriptBound(project, it)
        }
        it == model.scriptFile && model.transcriptFile == null -> {
          errorNoTranscriptBound(project, it)
        }
        else -> {
          DiffDialogFactory.showWindow(model)
        }
      }
    }
  }
}