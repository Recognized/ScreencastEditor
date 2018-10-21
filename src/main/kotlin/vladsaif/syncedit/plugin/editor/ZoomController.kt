package vladsaif.syncedit.plugin.editor

import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformView
import vladsaif.syncedit.plugin.editor.scriptview.LinearCoordinator
import vladsaif.syncedit.plugin.editor.scriptview.ScriptView
import vladsaif.syncedit.plugin.util.end
import java.awt.Dimension
import java.awt.Toolkit
import java.util.concurrent.TimeUnit
import javax.swing.JScrollPane

class ZoomController(val view: WaveformView?, val scriptView: ScriptView) {
  private var myScrollPane: JScrollPane? = null
  private var myIgnoreBarChanges = false
  @Volatile
  private var myBlockScaling = false
  private val myAcceptableScale = 10_000L..(Long.MAX_VALUE ushr 16)

  fun zoomIn() {
    scale(2f)
  }

  fun zoomOut() {
    scale(0.5f)
  }

  fun installZoom(scrollPane: JScrollPane) {
    myScrollPane = scrollPane
    val brm = scrollPane.horizontalScrollBar.model
    scrollPane.horizontalScrollBar.addAdjustmentListener {
      if (myIgnoreBarChanges) return@addAdjustmentListener
      view?.model?.setRangeProperties(visibleChunks = brm.extent, firstVisibleChunk = brm.value, maxChunks = brm.maximum)
      view?.model?.updateData()
    }
  }

  private fun scale(factor: Float) {
    if (myBlockScaling) return
    if (view != null) {
      scaleByAudio(factor)
    } else {
      scaleByScript(factor)
    }
  }

  private fun scaleByAudio(factor: Float) {
    myBlockScaling = true
    view?.model?.scale(factor) {
      myIgnoreBarChanges = true
      view.preferredSize = Dimension(view.model.maxChunks, view.height)
      val scrollPane = myScrollPane
      if (scrollPane != null) {
        val visible = scrollPane.viewport.visibleRect
        visible.x = view.model.firstVisibleChunk
        scrollPane.viewport.scrollRectToVisible(visible)
        scrollPane.horizontalScrollBar.value = visible.x
      }
      myIgnoreBarChanges = false
      view.selectionModel.resetSelection()
      view.model.fireStateChanged()
      revalidateRepaint()
      myBlockScaling = false
    }
  }

  private fun scaleByScript(factor: Float) {
    val linearCoordinator = scriptView.coordinator as LinearCoordinator
    val oldValue = linearCoordinator.getTimeUnitsPerPixel(TimeUnit.NANOSECONDS)
    linearCoordinator.setTimeUnitsPerPixel(
        (oldValue * factor).toLong().coerceIn(myAcceptableScale),
        TimeUnit.NANOSECONDS
    )
    val endTime = scriptView.screencast.codeBlockModel.blocks.lastOrNull()?.timeRange?.end ?: 0
    val scrollPane = myScrollPane
    if (scrollPane != null) {
      val currentPos = linearCoordinator.toNanoseconds(scrollPane.horizontalScrollBar.value)
      scriptView.preferredSize = Dimension(
          linearCoordinator.toScreenPixel(endTime.toLong(), TimeUnit.MILLISECONDS)
              + Toolkit.getDefaultToolkit().screenSize.width / 2,
          scriptView.height
      )
      val visible = scrollPane.viewport.visibleRect
      visible.x = linearCoordinator.toScreenPixel(currentPos, TimeUnit.NANOSECONDS)
      scrollPane.viewport.scrollRectToVisible(visible)
      scrollPane.horizontalScrollBar.value = visible.x
    }
  }

  private fun revalidateRepaint() {
    val scrollPane = myScrollPane ?: return
    scrollPane.viewport.view.revalidate()
    scrollPane.viewport.revalidate()
    scrollPane.revalidate()
    scrollPane.repaint()
    scrollPane.viewport.repaint()
    scrollPane.viewport.view.repaint()
  }
}