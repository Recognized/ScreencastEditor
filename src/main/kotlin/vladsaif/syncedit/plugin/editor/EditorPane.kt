package vladsaif.syncedit.plugin.editor

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.editor.audioview.StubPanel
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformController
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformView
import vladsaif.syncedit.plugin.editor.scriptview.ScriptView
import vladsaif.syncedit.plugin.model.ScreencastFile
import java.util.concurrent.TimeUnit
import javax.swing.ScrollPaneConstants
import javax.swing.event.ChangeListener

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
    waveformView?.installListeners()
    scriptView.installListeners()
    zoomController.installZoom(this)
    zoomController.addChangeListener(ChangeListener {
      scriptView.resetCache()
      val unitDistance = JBUI.scale(25)
      val interval = scriptView.coordinator.toNanoseconds(unitDistance)
      var x = 1L
      var temp = interval
      while (temp > 0) {
        temp /= 10
        x *= 10L
      }
      splitter.updateInterval(x, TimeUnit.NANOSECONDS)
    })
  }
}