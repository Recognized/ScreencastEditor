package vladsaif.syncedit.plugin.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import vladsaif.syncedit.plugin.model.Screencast

class ImportAudioAction(val screencast: Screencast) :
  AnAction("Import audio", "Import audio", AllIcons.ToolbarDecorator.Import) {

  override fun actionPerformed(e: AnActionEvent) {
    FileChooserDescriptorFactory.createSingleFileDescriptor().let {
      FileChooser.chooseFile(it, screencast.project, null) {

      }
    }
  }

  override fun update(e: AnActionEvent) {
    with(e.presentation) {
      isEnabled = true // TODO
    }
  }
}