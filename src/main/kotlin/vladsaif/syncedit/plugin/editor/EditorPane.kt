package vladsaif.syncedit.plugin.editor

import com.intellij.ui.components.JBScrollPane
import vladsaif.syncedit.plugin.editor.audioview.StubPanel
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformController
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformView
import vladsaif.syncedit.plugin.editor.scriptview.ScriptView
import vladsaif.syncedit.plugin.model.ScreencastFile
import javax.swing.ScrollPaneConstants

class EditorPane(
    screencast: ScreencastFile
) : JBScrollPane() {
  val waveformView = if (screencast.isAudioSet) WaveformView(screencast) else null
  val waveformController = waveformView?.let { WaveformController(it) }
  val scriptView = ScriptView(screencast, waveformView?.model?.coordinator)
  val zoomController = ZoomController(waveformView, scriptView)

  init {
    val splitter = EditorSplitter(
        waveformView ?: StubPanel(),
        scriptView,
        scriptView.coordinator
    )
    setViewportView(splitter)
    verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
    horizontalScrollBar.addAdjustmentListener {
      scriptView.repaint()
      splitter.repaint()
    }
    waveformView?.installListeners()
    zoomController.installZoom(this)
  }
}