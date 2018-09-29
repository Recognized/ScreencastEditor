package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import icons.ScreencastEditorIcons
import icons.ScreencastEditorIcons.*
import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.actions.errorRequirementsNotSatisfied
import vladsaif.syncedit.plugin.audioview.waveform.JScrollableWaveform
import vladsaif.syncedit.plugin.audioview.waveform.Player
import vladsaif.syncedit.plugin.audioview.waveform.Player.PlayState.PLAY
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException
import javax.swing.Icon

class AudioToolWindowPanel(screencast: ScreencastFile) : SimpleToolWindowPanel(false), Disposable {
  val wave = JScrollableWaveform(screencast)

  init {
    add(wave)
    val group = DefaultActionGroup()
    with(group) {
      with(wave.controller) {
        addAction("Play", "Play audio", ScreencastEditorIcons.PLAY, this::play) { playState != PLAY }
        addAction("Pause", "Pause audio", PAUSE, this::pause) { playState == PLAY }
        addAction("Stop", "Stop audio", STOP, this::stopImmediately) { playState != Player.PlayState.STOP }
        addAction("Undo", "Undo changes in selected area", AllIcons.Actions.Undo, this::undo) { hasSelection }
        addAction("Clip", "Clip audio", DELETE, this::cutSelected) { hasSelection }
        addAction("Mute", "Mute selected", VOLUME_OFF, this::muteSelected) { hasSelection }
        addAction("Zoom in", "Zoom in", AllIcons.Graph.ZoomIn, this::zoomIn) { true }
        addAction("Zoom out", "Zoom out", AllIcons.Graph.ZoomOut, this::zoomOut) { true }
        addAction(
            "Open transcript",
            "Open transcript in editor",
            EQUALIZER,
            this@AudioToolWindowPanel::openTranscript
        ) { true }
      }
    }
    setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false).component)
  }

  private fun openTranscript() {
    val screencast = wave.waveform.model.screencast
    val transcript = screencast.transcriptPsi
    if (transcript != null) {
      FileEditorManager.getInstance(screencast.project).openFile(transcript.virtualFile, true, true)
    } else {
      try {
        SpeechRecognizer.getCurrentRecognizer().checkRequirements()
        SpeechRecognizer.runRecognitionTask(screencast)
      } catch (ex: IOException) {
        errorRequirementsNotSatisfied(screencast.project, ex)
      }
    }
  }

  override fun dispose() {
    wave.controller.stopImmediately()
  }
}

fun DefaultActionGroup.addAction(what: String, desc: String?, icon: Icon?, action: () -> Unit, checkAvailable: () -> Boolean) {
  this.add(object : AnAction(what, desc, icon) {
    override fun actionPerformed(event: AnActionEvent) {
      action()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = checkAvailable()
    }
  })
}