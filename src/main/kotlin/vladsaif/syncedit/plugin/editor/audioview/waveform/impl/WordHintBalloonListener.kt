package vladsaif.syncedit.plugin.editor.audioview.waveform.impl

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.PositionTracker
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformModel
import vladsaif.syncedit.plugin.util.mulScale
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter

class WordHintBalloonListener(parent: Component, private val locator: WaveformModel) : MouseInputAdapter() {
  private var myPreviousBalloon: Balloon? = null
  private val myBalloonLabel = JBLabel()
  private var myBalloonPoint = Point(0, 0)
  private val myBalloonPositionTracker = object : PositionTracker<Balloon>(parent) {
    override fun recalculateLocation(baloon: Balloon?): RelativePoint {
      return RelativePoint(component, myBalloonPoint)
    }
  }
  private val myBalloon: Balloon
    get() {
      val prev = myPreviousBalloon
      return if (prev == null || prev.isDisposed || prev.wasFadedOut()) {
        JBPopupFactory.getInstance().createBalloonBuilder(myBalloonLabel)
          .setFadeoutTime(3000)
          .createBalloon()
          .also {
            it.setAnimationEnabled(false)
            myPreviousBalloon = it
          }
      } else {
        prev
      }
    }

  override fun mouseMoved(e: MouseEvent?) {
    e ?: return
    if (UIUtil.isControlKeyDown(e)) {
      myBalloonLabel.text = locator.getEnclosingWord(e.x.mulScale() + locator.pixelOffset)?.filteredText ?: return
      myBalloonPoint = e.point
      myBalloon.show(myBalloonPositionTracker, Balloon.Position.above)
      myBalloon.revalidate()
    }
  }
}
