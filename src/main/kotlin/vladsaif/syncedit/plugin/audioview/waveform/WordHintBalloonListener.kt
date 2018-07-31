package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.PositionTracker
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter

class WordHintBalloonListener(parent: Component, private val locator: WaveformModel) : MouseInputAdapter() {
    private var previousBalloon: Balloon? = null
    private val balloonLabel = JBLabel()
    private var balloonPoint = Point(0, 0)
    private val balloonPositionTracker = object : PositionTracker<Balloon>(parent) {
        override fun recalculateLocation(baloon: Balloon?): RelativePoint {
            return RelativePoint(component, balloonPoint)
        }
    }
    private val balloon: Balloon
        get() {
            val prev = previousBalloon
            return if (prev == null || prev.isDisposed || prev.wasFadedOut()) {
                JBPopupFactory.getInstance().createBalloonBuilder(balloonLabel)
                        .setFadeoutTime(3000)
                        .createBalloon()
                        .also {
                            it.setAnimationEnabled(false)
                            previousBalloon = it
                        }
            } else {
                prev
            }
        }

    override fun mouseMoved(e: MouseEvent?) {
        e ?: return
        if (UIUtil.isControlKeyDown(e)) {
            balloonLabel.text = locator.getEnclosingWord(e.x)?.filteredText ?: return
            balloonPoint = e.point
            balloon.show(balloonPositionTracker, Balloon.Position.above)
            balloon.revalidate()
        }
    }
}
