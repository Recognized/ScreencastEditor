package com.github.recognized.screencast.editor.view.audioview.waveform.impl

import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent

abstract class DragXAxisListener : MouseDragListener() {
  private var myDragStarted = false
  private var myTempMovingDelta: Int = 0

  abstract fun onDragAction()

  abstract fun onDragFinishedAction(delta: Int)

  val delta: Int get() = myTempMovingDelta

  override fun mouseMoved(e: MouseEvent?) {
    e?.component?.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
  }

  override fun onDragStarted(point: Point) {
    super.onDragStarted(point)
    if (!dragStartEvent!!.isShiftDown && !UIUtil.isControlKeyDown(dragStartEvent!!)) {
      myDragStarted = true
    }
  }

  override fun onDrag(point: Point) {
    super.onDrag(point)
    if (myDragStarted) {
      myTempMovingDelta = point.x - dragStartEvent!!.x
      onDragAction()
    }
  }

  override fun onDragFinished(point: Point) {
    super.onDragFinished(point)
    if (myDragStarted) {
      val delta = myTempMovingDelta
      myTempMovingDelta = 0
      myDragStarted = false
      onDragFinishedAction(delta)
    }
  }
}