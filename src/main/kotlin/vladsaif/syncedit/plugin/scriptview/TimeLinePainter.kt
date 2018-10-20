package vladsaif.syncedit.plugin.scriptview

import com.intellij.openapi.ui.AbstractPainter
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.math.BigDecimal
import java.util.concurrent.TimeUnit


class TimeLinePainter(private val coordinator: Coordinator) : AbstractPainter() {
  // Interval between adjacent time marks on time line in nanoseconds
  private var myInterval: Long = 1_000_000_000 // 1 second
  private val myDivisor = BigDecimal.valueOf(1_000_000_000)

  override fun executePaint(component: Component, g: Graphics2D) {
    val bounds = g.clipBounds ?: Rectangle(0, 0, component.width, component.height)
    with(g) {
      stroke = BasicStroke(STROKE_WIDTH)
      color = Color.black
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
      val currentPos = coordinator.toScreenPixel(timeStart / halfInterval, TimeUnit.NANOSECONDS)
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
      val currentPos = coordinator.toScreenPixel(timeStart / myInterval, TimeUnit.NANOSECONDS)
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

  data class SizedString(val value: String, val width: Int, val height: Int) {
    override fun toString() = value
  }

  companion object {
    private val STROKE_WIDTH = 1.0f
    private val BIG_MARK_HEIGHT = 3.0f
    private val SMALL_MARK_HEIGHT = 1.5f
  }
}