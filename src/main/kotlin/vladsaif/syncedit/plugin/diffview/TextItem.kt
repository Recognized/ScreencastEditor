package vladsaif.syncedit.plugin.diffview

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.TextFormatter
import java.awt.*
import javax.swing.BorderFactory
import kotlin.math.max
import kotlin.math.min

class TextItem(val text: String) : JBPanel<TextItem>() {
  private val availableWidth get() = max(width - with(insets) { left + right } - 10.scale(), 0)
  private var textColor: Color = Color(0, 150, 0)
  private var textColorDark: Color = Color(106, 135, 89)
  private var myCharSize: Int
  private val myDarkBackground: Color = Color(43, 43, 43)
  private val myBackground: Color = Color.WHITE

  init {
    font = JBUI.Fonts.create(Font.MONOSPACED, 12).asBold()
    myCharSize = getFontMetrics(font).charWidth('m')
    border = BorderFactory.createEmptyBorder(
        2.scale(),
        (TextItem.Companion.RADIUS / 2).scale(),
        2.scale(),
        (TextItem.Companion.RADIUS / 2).scale()
    )
  }

  override fun getMaximumSize(): Dimension {
    return Dimension(parent.width, getDesiredHeight(parent.width))
  }

  override fun getMinimumSize(): Dimension {
    return maximumSize
  }

  fun getDesiredHeight(width: Int): Int {
    val metrics = getFontMetrics(font)
    val lines = TextFormatter.splitText(text, max(width - insets.left - insets.right - 10.scale(), 0)) {
      it.length * myCharSize
    }
    return lines.size * metrics.height + insets.top + insets.bottom + metrics.height / 2
  }

  override fun getPreferredSize(): Dimension {
    return Dimension(parent.width, getDesiredHeight(parent.width))
  }

  override fun paintComponent(g: Graphics) {
    with(g as Graphics2D) {
      setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      color = if (UIUtil.isUnderDarcula()) myDarkBackground else myBackground
      fillRect(0, 0, width, height)
      val metrics = getFontMetrics(font)
      myCharSize = metrics.charWidth('m')
      val getWidth = { string: String -> string.length * myCharSize }
      val lines = TextFormatter.splitText(text, availableWidth, getWidth)
      val lineHeight = getFontMetrics(font).height
      val availableLines = (height - insets.top - insets.bottom) / lineHeight
      val needDraw = lines.subList(0, max(min(availableLines, lines.size), 0)).map { line ->
        TextFormatter.createEllipsis(line, availableWidth, getWidth)
      }.toMutableList()
      if (needDraw.size != lines.size && !needDraw.isEmpty()) {
        needDraw[needDraw.size - 1] = TextFormatter.createEllipsis(needDraw.last() + "...", availableWidth, getWidth)
      }
      color = if (UIUtil.isUnderDarcula()) textColorDark else textColor
      drawLines(needDraw)
    }
  }

  private fun Graphics2D.drawLines(lines: List<String>) {
    val dy = getFontMetrics(font).height
    var y = dy + insets.top
    val leftInset = insets.left
    for (line in lines) {
      drawString(line, leftInset, y)
      y += dy
    }
  }

  private fun Int.scale() = JBUI.scale(this)

  companion object {

    private const val RADIUS: Int = 10
  }
}