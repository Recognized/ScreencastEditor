package vladsaif.syncedit.plugin.actions.internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import vladsaif.syncedit.plugin.diffview.GridBagBuilder
import vladsaif.syncedit.plugin.format.ScreencastZipper
import vladsaif.syncedit.plugin.sound.CountdownPanel
import vladsaif.syncedit.plugin.sound.OutlinePanel
import vladsaif.syncedit.plugin.sound.SoundRecorder
import vladsaif.syncedit.plugin.sound.SoundRecorder.State.IDLE
import vladsaif.syncedit.plugin.sound.SoundRecorder.State.RECORDING
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files

object RecordingManager {
  private val COUNTDOWN_LISTENER_KEY = Key.create<SoundRecorder.StateListener>("countdown_listener")
  private val OUTLINE_LISTENER_KEY = Key.create<SoundRecorder.StateListener>("countdown_listener")

  fun startRecording(project: Project) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val countdown = createCountdownListener(project)
    val outlinePanel = OutlinePanel()
    val outlineListener = createOutlineListener(outlinePanel)
    val ideFrame = WindowManager.getInstance().getIdeFrame(project)
    val glassPane = IdeGlassPaneUtil.find(ideFrame.component) as IdeGlassPaneImpl
    glassPane.layout = GridBagLayout()
    glassPane.add(
        outlinePanel,
        GridBagBuilder()
            .weightx(1.0)
            .weighty(1.0)
            .gridx(0)
            .gridy(0)
            .fill(GridBagConstraints.BOTH)
            .done()
    )
    glassPane.revalidate()
    glassPane.repaint()
    addListener(project, countdown, COUNTDOWN_LISTENER_KEY)
    addListener(project, outlineListener, OUTLINE_LISTENER_KEY)
    val zipper = ScreencastZipper(Files.createTempFile("screencast", ""))
    SoundRecorder.start(project) { audioInput ->
      zipper.useAudioOutputStream { audioOutput ->
        //        AudioSystem.write(audioInput, AudioFileFormat.Type, audioOutput.buffered())
      }
      zipper.addScript("Hello")
    }
  }

  fun stopRecording(project: Project) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    SoundRecorder.stop()
    removeListener(project, COUNTDOWN_LISTENER_KEY)
    removeListener(project, OUTLINE_LISTENER_KEY)
  }

  fun pauseRecording(project: Project) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    SoundRecorder.pause()
  }

  private fun createCountdownListener(project: Project) = object : SoundRecorder.StateListener {
    override fun stateChanged(oldValue: SoundRecorder.State, newValue: SoundRecorder.State) {
      if (oldValue == IDLE && newValue == RECORDING) {
        val ideFrame = WindowManager.getInstance().getIdeFrame(project)
        val glassPane = IdeGlassPaneUtil.find(ideFrame.component) as IdeGlassPaneImpl
        val countDown = CountdownPanel(3)
        countDown.deactivationAction = {
          glassPane.remove(countDown)
          glassPane.revalidate()
          glassPane.repaint()
        }
        glassPane.add(
            countDown,
            GridBagBuilder()
                .weightx(1.0)
                .weighty(1.0)
                .gridx(0)
                .gridy(0)
                .fill(GridBagConstraints.BOTH)
                .done()
        )
        glassPane.revalidate()
        glassPane.repaint()
        countDown.countDown()
      }
    }
  }

  private fun createOutlineListener(panel: OutlinePanel) = object : SoundRecorder.StateListener {
    override fun stateChanged(oldValue: SoundRecorder.State, newValue: SoundRecorder.State) {
      panel.updateState(newValue)
    }
  }

  private fun <T : SoundRecorder.StateListener> addListener(project: Project, listener: T, key: Key<T>) {
    SoundRecorder.addListener(listener)
    project.putUserData(key, listener)
  }

  private fun <T : SoundRecorder.StateListener> removeListener(project: Project, key: Key<T>) {
    val listener = project.getUserData(key)
    if (listener != null) {
      SoundRecorder.removeListener(listener)
    }
  }
}