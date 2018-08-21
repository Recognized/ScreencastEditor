package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import icons.ScreencastEditorIcons
import icons.ScreencastEditorIcons.*
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.actions.RecognizeAudioAction
import vladsaif.syncedit.plugin.audioview.waveform.JScrollableWaveform
import vladsaif.syncedit.plugin.audioview.waveform.Player
import vladsaif.syncedit.plugin.audioview.waveform.Player.PlayState.PLAY
import vladsaif.syncedit.plugin.recognition.CredentialProvider
import javax.swing.Icon

class AudioToolWindowPanel(multimediaModel: MultimediaModel) : SimpleToolWindowPanel(false), Disposable {
  val wave = JScrollableWaveform(multimediaModel)

  init {
    add(wave)
    val group = DefaultActionGroup()
    with(wave.controller) {
      // Maybe, actions should be placed in two groups
      group.addAction("Play", "Play audio", ScreencastEditorIcons.PLAY, this::play) { playState != PLAY }
      group.addAction("Pause", "Pause audio", PAUSE, this::pause) { playState == PLAY }
      group.addAction("Stop", "Stop audio", STOP, this::stopImmediately) { playState != Player.PlayState.STOP }
      group.addAction("Undo", "Undo changes in selected area", AllIcons.Actions.Undo, this::undo) { hasSelection }
      group.addAction("Clip", "Clip audio", DELETE, this::cutSelected) { hasSelection }
      group.addAction("Mute", "Mute selected", VOLUME_OFF, this::muteSelected) { hasSelection }
      group.addAction("Zoom in", "Zoom in", AllIcons.Graph.ZoomIn, this::zoomIn) { true }
      group.addAction("Zoom out", "Zoom out", AllIcons.Graph.ZoomOut, this::zoomOut) { true }
      group.addAction(
          "Open transcript",
          "Open transcript in editor",
          EQUALIZER,
          this@AudioToolWindowPanel::openTranscript
      ) { true }
    }
    setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false).component)
  }

  private fun openTranscript() {
    with(wave.waveform.model.multimediaModel) {
      val xml = xmlFile
      val audio = audioFile ?: return
      if (xml != null) {
        // We may do not perform xml update now because its content is maintained
        // synchronized with transcript on each modification event.
        // updateXml()
        FileEditorManager.getInstance(project).openFile(xml, true, true)
      } else {
        if (CredentialProvider.Instance.gSettings == null) {
          RecognizeAudioAction.showNoCredentialsDialog(project)
        } else {
          RecognizeAudioAction.runRecognitionTask(project, this, audio)
        }
      }
    }
  }

  override fun dispose() {
    wave.controller.stopImmediately()
  }
}

fun DefaultActionGroup.addAction(what: String, desc: String?, icon: Icon?, action: () -> Unit, checkAvailable: () -> Boolean) {
  this.add(object : AnAction(what, desc, icon) {
    override fun actionPerformed(event: AnActionEvent?) {
      action()
    }

    override fun update(e: AnActionEvent?) {
      e?.presentation?.isEnabled = checkAvailable()
    }
  })
}