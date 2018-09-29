package vladsaif.syncedit.plugin.actions.project

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import vladsaif.syncedit.plugin.actions.tools.OpenScreencast
import vladsaif.syncedit.plugin.format.ScreencastFileType

class OpenScreencastFromProjectTree : OpenScreencast() {

  override fun actionPerformed(e: AnActionEvent) {
    val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
    open(e.project!!, file)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.presentation.isEnabledAndVisible
        && e.project != null
        && e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == ScreencastFileType
  }
}
