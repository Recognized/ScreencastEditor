package vladsaif.syncedit.plugin.diffview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.script.psi.UIScriptFileType

class OpenDiffAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)

  }

  override fun update(e: AnActionEvent?) {
    e ?: return
    val editor = e.dataContext.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE)
    val file = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    e.presentation.isEnabledAndVisible =
        editor != null &&
        file != null &&
        file.fileType == UIScriptFileType &&
        MultimediaModel.get(file)?.transcriptPsi != null
  }

  companion object {
    fun openDiff(model: MultimediaModel) {
      val script = model.scriptFile
      val transcriptData = model.data
      if (script == null || transcriptData == null) {
        // TODO
        return
      }
    }
  }
}