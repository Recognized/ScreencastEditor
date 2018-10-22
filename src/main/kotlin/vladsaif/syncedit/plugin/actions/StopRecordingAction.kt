package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.testGuiFramework.recorder.GlobalActionRecorder
import vladsaif.syncedit.plugin.actions.internal.RecordingManager

class StopRecordingAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    GlobalActionRecorder.deactivate()
    Timer.stop()
    RecordingManager.stopRecording()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = GlobalActionRecorder.isActive
  }
}
