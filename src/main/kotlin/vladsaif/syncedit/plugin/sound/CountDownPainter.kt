package vladsaif.syncedit.plugin.sound

import com.intellij.openapi.ui.AbstractPainter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Timer


class CountDownPainter(count: Int) : AbstractPainter() {
  private val myCounter = AtomicInteger(count)
  private val myTimer = Timer(1000) {
    myCounter.decrementAndGet()
    if (myDeactivateNextTime.get()) {
      deactivate()
      return@Timer
    }
    setNeedsRepaint(true)
  }
  private var myDeactivateNextTime = AtomicBoolean(false)
  var deactivationAction: (() -> Unit)? = null

  private fun deactivate() {
    myTimer.stop()
    deactivationAction?.invoke()
  }

  override fun executePaint(component: Component?, g: Graphics2D?) {
    component ?: return
    g ?: return
    with(g) {
      font = UIUtil.getLabelFont().deriveFont(Font.BOLD).deriveFont(JBUI.scale(100.0f))
      stroke = BasicStroke(JBUI.scale(4.0f))
      val metrics = getFontMetrics(font)
      val count = myCounter.get()
      val str = if (count > 0) "" + count else "GO"
      if (count == 0) {
        myDeactivateNextTime.compareAndSet(false, true)
      }
      val gv = font.layoutGlyphVector(fontRenderContext, str.toCharArray(), 0, str.length, 0)
      val shape = gv.outline
      Color.BLACK
      translate(
          component.width / 2 - metrics.stringWidth(str) / 2,
          component.height / 3 + metrics.height
      )
      draw(shape)
      color = Color.YELLOW
      drawString(str, 0, 0)
    }
  }

  fun countDown() {
    setNeedsRepaint(true)
    myTimer.start()
  }
}