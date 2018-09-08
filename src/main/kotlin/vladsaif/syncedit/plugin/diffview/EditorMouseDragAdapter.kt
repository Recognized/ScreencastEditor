package vladsaif.syncedit.plugin.diffview

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import vladsaif.syncedit.plugin.audioview.waveform.impl.MouseDragListener

class EditorMouseDragAdapter(private val delegate: MouseDragListener) : EditorMouseListener, EditorMouseMotionListener {

  override fun mouseReleased(e: EditorMouseEvent) {
    delegate.mouseReleased(e.mouseEvent)
  }

  override fun mouseMoved(e: EditorMouseEvent) {
    delegate.mouseMoved(e.mouseEvent)
  }

  override fun mouseEntered(e: EditorMouseEvent) {
    delegate.mouseEntered(e.mouseEvent)
  }

  override fun mouseDragged(e: EditorMouseEvent) {
    delegate.mouseDragged(e.mouseEvent)
  }

  override fun mouseClicked(e: EditorMouseEvent) {
    delegate.mouseClicked(e.mouseEvent)
  }

  override fun mouseExited(e: EditorMouseEvent) {
    delegate.mouseExited(e.mouseEvent)
  }

  override fun mousePressed(e: EditorMouseEvent) {
    delegate.mousePressed(e.mouseEvent)
  }


}