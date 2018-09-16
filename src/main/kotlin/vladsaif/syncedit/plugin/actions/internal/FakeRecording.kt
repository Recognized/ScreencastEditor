package vladsaif.syncedit.plugin.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vladsaif.syncedit.plugin.SoundRecorder
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class FakeRecording : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val path = Paths.get(e.project!!.basePath, "heheh.wav")
    println(path.toAbsolutePath())
    SoundRecorder.startRecording(path)
    launch {
      delay(3000, TimeUnit.MILLISECONDS)
      ApplicationManager.getApplication().invokeLater {
        SoundRecorder.stopRecording()
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = ApplicationManager.getApplication().isInternal
  }
}