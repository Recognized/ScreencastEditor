package vladsaif.syncedit.plugin.actions.project

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import kotlinx.coroutines.experimental.launch
import vladsaif.syncedit.plugin.actions.tools.OpenAudioAction

class OpenAudioProjectTreeAction : OpenAudioAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
    launch {
      OpenAudioAction.openAudio(e.project!!, file)
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabled = e.presentation.isEnabled
        && e.getData(CommonDataKeys.VIRTUAL_FILE) != null
        && e.project != null
  }
}
