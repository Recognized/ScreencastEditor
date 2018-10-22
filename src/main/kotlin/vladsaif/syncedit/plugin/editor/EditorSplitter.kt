package vladsaif.syncedit.plugin.editor

import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.util.ui.UIUtil
import gnu.trove.TLongObjectHashMap
import vladsaif.syncedit.plugin.editor.scriptview.Coordinator
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.BIG_MARK_HEIGHT
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.SMALL_MARK_HEIGHT
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.STROKE_WIDTH
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

class EditorSplitter(
    waveformView: JComponent,
    scriptView: JComponent,
    private val coordinator: Coordinator
) : Splitter(true, 0.5f, 0.0f, 1.0f) {
  private var myInterval: Long = 1_000_000_000 // 1 second
  private val myFormatCache = TLongObjectHashMap<SizedString>()

  init {
    firstComponent = waveformView
    secondComponent = scriptView
    dividerWidth = getFontMetrics(UIUtil.getLabelFont()).height + (BIG_MARK_HEIGHT * 2).toInt()
  }

  fun updateInterval(time: Long, unit: TimeUnit) {
    myInterval = TimeUnit.NANOSECONDS.convert(time, unit)
    myFormatCache.clear()
  }

  override fun createDivider(): Divider {
    return object : DividerImpl() {
      // Interval between adjacent time marks on time line in nanoseconds
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
          var sizedString: SizedString? = myFormatCache.get(timeStart)
          if (sizedString == null) {
            sizedString = formatTime(timeStart)
            myFormatCache.put(timeStart, sizedString)
          }
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
        var string = (BigDecimal.valueOf(ns).divide(myDivisor, 10, RoundingMode.HALF_UP))
            .toString()
            .trimEnd { it == '0' }
        if (string.endsWith('.')) {
          string += '0'
        }
        val width = fontMetrics.stringWidth(string)
        val height = fontMetrics.height
        return SizedString(string, width, height)
      }
    }
  }

  data class SizedString(val value: String, val width: Int, val height: Int) {
    override fun toString() = value
  }
}