package vladsaif.syncedit.plugin.util

import com.intellij.util.ui.JBUI
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import kotlin.math.roundToInt

interface DrawingFixture {

  fun Graphics2D.drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

  fun Graphics2D.fillRect(x1: Float, y1: Float, width: Float, height: Float)

  companion object {

    fun create() = object : DrawingFixture {
      private val mySharedRect = Rectangle2D.Float()
      private val mySharedLine = Line2D.Float()

      override fun Graphics2D.drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        mySharedLine.setLine(x1, y1, x2, y2)
        draw(mySharedLine)
      }

      override fun Graphics2D.fillRect(x1: Float, y1: Float, width: Float, height: Float) {
        mySharedRect.setRect(x1, y1, width, height)
        fill(mySharedRect)
      }
    }
  }
}

fun Int.divScale(): Int {
  return (this / JBUI.pixScale()).roundToInt()
}

fun Int.mulScale(): Int {
  return (JBUI.pixScale(this.toFloat())).roundToInt()
}

fun Int.divScaleF(): Float {
  return this / JBUI.pixScale()
}

fun Int.mulScaleF(): Float {
  return JBUI.pixScale(this.toFloat())
}
