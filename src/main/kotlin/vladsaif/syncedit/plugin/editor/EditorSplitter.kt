package vladsaif.syncedit.plugin.editor

import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.scriptview.Coordinator
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.BIG_MARK_HEIGHT
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.SMALL_MARK_HEIGHT
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.STROKE_WIDTH
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

class EditorSplitter(
    waveformView: JComponent,
    scriptView: JComponent,
    private val coordinator: Coordinator
) : Splitter(true, 0.5f, 0.0f, 1.0f) {

  init {
    firstComponent = waveformView
    secondComponent = scriptView
    dividerWidth = getFontMetrics(UIUtil.getLabelFont()).height + (BIG_MARK_HEIGHT * 2).toInt()
  }

  override fun createDivider(): Divider {
    return object : DividerImpl() {
      // Interval between adjacent time marks on time line in nanoseconds
      private var myInterval: Long = 1_000_000_000 // 1 second
      private val myDivisor = BigDecimal.valueOf(1_000_000_000)

      override fun paint(g: Graphics) {
        val bounds = Rectangle(0, 0, width, height)
        with(g as Graphics2D) {
          stroke = BasicStroke(STROKE_WIDTH)
          color = UIUtil.getLabelFontColor(UIUtil.FontColor.NORMAL)
          drawLine(bounds.x, bounds.height, bounds.width, bounds.height)
          drawHalfInterval(bounds)
          drawInterval(bounds)
        }
      }

      private fun Graphics2D.drawHalfInterval(bounds: Rectangle) {
        val start = bounds.x
        val end = bounds.x + bounds.width
        val halfInterval = myInterval / 2
        var timeStart = (coordinator.toNanoseconds(start) ceil halfInterval) * halfInterval // align mark
        val timeEnd = coordinator.toNanoseconds(end)
        while (timeStart <= timeEnd) {
          val currentPos = coordinator.toScreenPixel(timeStart, TimeUnit.NANOSECONDS)
          drawLine(currentPos, bounds.height, currentPos, (bounds.height - SMALL_MARK_HEIGHT).toInt())
          timeStart += halfInterval
        }
      }

      private fun Graphics2D.drawInterval(bounds: Rectangle) {
        val start = bounds.x
        val end = bounds.x + bounds.width
        var timeStart = (coordinator.toNanoseconds(start) ceil myInterval) * myInterval
        val timeEnd = coordinator.toNanoseconds(end)
        while (timeStart <= timeEnd) {
          val currentPos = coordinator.toScreenPixel(timeStart, TimeUnit.NANOSECONDS)
          drawLine(currentPos, bounds.height, currentPos, (bounds.height - BIG_MARK_HEIGHT).toInt())
          val sizedString = formatTime(timeStart)
          font = UIUtil.getLabelFont()
          drawString(
              sizedString.value,
              currentPos - sizedString.width / 2,
              (bounds.height - BIG_MARK_HEIGHT * 1.5).toInt()
          )
          timeStart += myInterval
        }
      }

      private infix fun Long.ceil(other: Long) = (this + other - 1) / other

      private fun Graphics2D.formatTime(ns: Long): SizedString {
        val string = (BigDecimal.valueOf(ns) / myDivisor).toString()
        val width = fontMetrics.stringWidth(string)
        val height = fontMetrics.height
        return SizedString(string, width, height)
      }
    }
  }

  data class SizedString(val value: String, val width: Int, val height: Int) {
    override fun toString() = value
  }

  companion object {

  }
}