package vladsaif.syncedit.plugin.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import vladsaif.syncedit.plugin.sound.CountDownPainter
import vladsaif.syncedit.plugin.sound.SoundRecorder
import vladsaif.syncedit.plugin.sound.SoundRecorderListener
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class FakeRecording : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return;
    val path = Paths.get(project.basePath, "heheh.wav")
    println(path.toAbsolutePath())
    SoundRecorder.startRecording(path, project, object : SoundRecorderListener {
      override fun beforeRecordingStart() {
//        TODO("not implemented")
      }

      override fun recordingStarted() {
        ApplicationManager.getApplication().invokeAndWait {
          val ideFrame = WindowManager.getInstance().getIdeFrame(project)
          val glassPane = IdeGlassPaneUtil.find(ideFrame.component) as IdeGlassPaneImpl
          val countDown = CountDownPainter(3)
          countDown.deactivationAction = { glassPane.removePainter(countDown) }
          glassPane.addPainter(glassPane, countDown) { /* nothing */ }
          countDown.countDown()
        }
      }

      override fun handleError(exception: IOException) {
//        TODO("not implemented")
      }
    })
    launch {
      delay(5000, TimeUnit.MILLISECONDS)
      ApplicationManager.getApplication().invokeLater {
        SoundRecorder.stopRecording()
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = ApplicationManager.getApplication().isInternal
  }
}