package com.github.recognized.screencast.editor.view

import com.github.recognized.kotlin.ranges.extensions.length
import com.github.recognized.screencast.editor.util.divScale
import com.github.recognized.screencast.editor.util.mulScale
import com.github.recognized.screencast.editor.view.audioview.waveform.ChangeNotifier
import com.github.recognized.screencast.editor.view.audioview.waveform.impl.DefaultChangeNotifier
import java.util.concurrent.TimeUnit
import javax.swing.JScrollPane
import javax.swing.event.ChangeListener
import kotlin.math.max

class ZoomController(val coordinator: Coordinator) : ChangeNotifier by DefaultChangeNotifier() {

  private lateinit var myScrollPane: JScrollPane

  init {
    addChangeListener(ChangeListener {
      with(myScrollPane) {
        viewport.view.revalidate()
        viewport.revalidate()
        revalidate()
        repaint()
        viewport.repaint()
        viewport.view.repaint()
      }
    })
  }

  fun zoomIn() {
    scale(2f)
  }

  fun zoomOut() {
    scale(0.5f)
  }

  fun install(scrollPane: JScrollPane) {
    myScrollPane = scrollPane
    scrollPane.horizontalScrollBar.addAdjustmentListener {
      coordinator.visibleRange = with(it.adjustable) {
        value..value + visibleAmount
      }
    }
  }

  private fun scale(factor: Float) {
    val currentCenterPixel = with(coordinator.visibleRange) { (start + endInclusive) / 2 }
    val currentCenterTime = coordinator.toNanoseconds(currentCenterPixel.mulScale())
    coordinator.framesPerPixel = (coordinator.framesPerPixel / factor).toLong()
    with(myScrollPane) {
      val visible = viewport.visibleRect
      val newCenterPixel = coordinator.toPixel(currentCenterTime, TimeUnit.NANOSECONDS).divScale()
      visible.x = max(newCenterPixel - coordinator.visibleRange.length / 2, 0)
      viewport.scrollRectToVisible(visible)
      horizontalScrollBar.value = visible.x
    }
    fireStateChanged()
  }
}