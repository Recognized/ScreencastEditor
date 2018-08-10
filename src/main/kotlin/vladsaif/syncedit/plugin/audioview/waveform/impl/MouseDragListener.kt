package vladsaif.syncedit.plugin.audioview.waveform.impl

import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter

abstract class MouseDragListener : MouseInputAdapter() {
  private var myDragStarted: Boolean = false
  var dragStartEvent: MouseEvent? = null
    private set

  abstract fun onDragStarted(point: Point)

  abstract fun onDrag(point: Point)

  abstract fun onDragFinished(point: Point)

  override fun mouseReleased(e: MouseEvent?) {
    doEnd(e)
  }

  override fun mouseMoved(e: MouseEvent?) {
    doEnd(e)
  }

  private fun doEnd(e: MouseEvent?) {
    e ?: return
    if (myDragStarted) {
      onDragFinished(e.point)
      myDragStarted = false
      dragStartEvent = null
    }
  }

  override fun mouseEntered(e: MouseEvent?) {
    doEnd(e)
  }

  override fun mouseDragged(e: MouseEvent?) {
    e ?: return
    if (dragStartEvent?.modifiers != e.modifiers) {
      onDragFinished(e.point)
      myDragStarted = false
      dragStartEvent = null
    }
    if (!myDragStarted) {
      myDragStarted = true
      dragStartEvent = e
      onDragStarted(e.point)
    }
    onDrag(e.point)
  }

  override fun mouseClicked(e: MouseEvent?) {
    doEnd(e)
  }

  override fun mouseExited(e: MouseEvent?) {
    doEnd(e)
  }

  override fun mousePressed(e: MouseEvent?) {
    doEnd(e)
  }
}