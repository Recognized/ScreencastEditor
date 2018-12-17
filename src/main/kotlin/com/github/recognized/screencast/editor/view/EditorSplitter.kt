package com.github.recognized.screencast.editor.view

import com.github.recognized.kotlin.ranges.extensions.intersects
import com.github.recognized.kotlin.ranges.extensions.length
import com.github.recognized.kotlin.ranges.extensions.mapInt
import com.github.recognized.screencast.editor.util.*
import com.github.recognized.screencast.editor.view.scriptview.ScriptGraphics.BIG_MARK_HEIGHT
import com.github.recognized.screencast.editor.view.scriptview.ScriptGraphics.SMALL_MARK_HEIGHT
import com.github.recognized.screencast.editor.view.scriptview.ScriptGraphics.STROKE_WIDTH
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.util.ui.UIUtil
import gnu.trove.TLongObjectHashMap
import java.awt.*
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

class EditorSplitter(
  waveforms: JComponent,
  scriptView: JComponent,
  private val coordinator: Coordinator
) :
  Splitter(true, 0.5f, 0.0f, 1.0f) {

  private var myInterval: Long = 1_000_000_000 // 1 second
  private var myDropLast: Int = 8
    set(value) {
      field = value.coerceIn(0..8)
    }
  private val myFormatCache = TLongObjectHashMap<SizedString>()

  init {
    firstComponent = waveforms
    secondComponent = scriptView
    dividerWidth = getFontMetrics(UIUtil.getLabelFont()).height + (BIG_MARK_HEIGHT * 2).toInt()
  }

  fun updateInterval() {
    val width = width.mulScale()
    val totalNanoseconds = coordinator.toNanoseconds(width)
    var interval = 1L
    var dropLast: Int
    var five = true
    do {
      interval *= if (five) 5 else 2
      dropLast = generateSequence(interval) { it / 10 }.takeWhile { it > 0 }.count().coerceIn(1..9) - 1
      five = five xor true
      val lastTime = (totalNanoseconds / interval) * interval
      val lastTimeWidth = graphics.fontMetrics.stringWidth(TextFormatter.formatTime(lastTime).dropLast(dropLast))
      val intervalWidth = coordinator.toPixel(interval - 1, TimeUnit.NANOSECONDS).divScale()
    } while (intervalWidth < lastTimeWidth * 3)
    LOG.info("Interval updated: $myInterval -> $interval")
    myInterval = interval
    myDropLast = dropLast
    myFormatCache.clear()
  }

  override fun createDivider(): Divider {
    return object : DividerImpl(), DrawingFixture by DrawingFixture.create() {
      private var myLastDrawnRange: IntRange? = null
      private var myIsFirstTimeDrawing = true

      override fun paint(g: Graphics) {
        if (myIsFirstTimeDrawing) {
          updateInterval()
          myIsFirstTimeDrawing = false
        }
        val visibleRange = coordinator.visibleRange.mapInt { it.mulScale() }
        val delta = visibleRange.length * 3
        val expandedStart = max(visibleRange.start - delta, 0)
        val expandedEnd = min(visibleRange.endInclusive + delta, width.mulScale() + 1)
        val bounds = Rectangle(expandedStart, 0, expandedEnd, height)
        if (myLastDrawnRange?.intersects(expandedStart..expandedEnd) != true) {
          myFormatCache.clear()
        }
        myLastDrawnRange = expandedStart..expandedEnd
        with(g as Graphics2D) {
          setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
          stroke = BasicStroke(STROKE_WIDTH)
          color = UIUtil.getLabelFontColor(UIUtil.FontColor.NORMAL)
          drawLine(
            bounds.x.divScaleF(),
            bounds.height.toFloat(),
            bounds.width.divScaleF(),
            bounds.height.toFloat()
          )
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
          val currentPos = coordinator.toPixel(timeStart, TimeUnit.NANOSECONDS).divScaleF()
          drawLine(
            currentPos,
            bounds.height.toFloat(),
            currentPos,
            bounds.height - SMALL_MARK_HEIGHT
          )
          timeStart += halfInterval
        }
      }

      private fun Graphics2D.drawInterval(bounds: Rectangle) {
        val start = bounds.x
        val end = bounds.x + bounds.width
        var timeStart = max((coordinator.toNanoseconds(start) ceil myInterval) * myInterval, 0)
        val timeEnd = coordinator.toNanoseconds(end)
        while (timeStart <= timeEnd) {
          val currentPos = coordinator.toPixel(timeStart, TimeUnit.NANOSECONDS).divScaleF()
          drawLine(
            currentPos,
            bounds.height.toFloat(),
            currentPos,
            bounds.height - BIG_MARK_HEIGHT
          )
          var sizedString: SizedString? = myFormatCache.get(timeStart)
          if (sizedString == null) {
            sizedString = formatTime(timeStart)
            myFormatCache.put(timeStart, sizedString)
          }
          font = UIUtil.getLabelFont()
          drawString(
            sizedString.value,
            currentPos - sizedString.width / 2,
            bounds.height - BIG_MARK_HEIGHT * 1.5f
          )
          timeStart += myInterval
        }
      }

      private fun Graphics2D.formatTime(ns: Long): SizedString {
        val string = TextFormatter.formatTime(ns).dropLast(myDropLast)
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
    private val LOG = logger<EditorSplitter>()
  }
}