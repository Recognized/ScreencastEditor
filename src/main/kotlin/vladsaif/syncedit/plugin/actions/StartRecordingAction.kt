package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.testGuiFramework.recorder.GlobalActionRecorder
import vladsaif.syncedit.plugin.actions.internal.RecordingManager

class StartRecordingAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    Timer.start()
    RecordingManager.startRecording()
    GlobalActionRecorder.activate()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !GlobalActionRecorder.isActive
  }
}
