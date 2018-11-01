package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import vladsaif.syncedit.plugin.actions.internal.RecordingManager

class StopRecordingAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    RecordingManager.stopRecording()
  }

  override fun update(e: AnActionEvent) {
//    e.presentation.isEnabled = GlobalActionRecorder.isActive
  }
}
