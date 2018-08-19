package vladsaif.syncedit.plugin.diffview

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.Settings
import vladsaif.syncedit.plugin.TextFormatter
import java.awt.*
import javax.swing.BorderFactory
import kotlin.math.max
import kotlin.math.min

class TextItem(
    val text: String
) : JBPanel<TextItem>() {
  private val availableWidth get() = max(width - with(insets) { left + right } - 10.scale(), 0)
  private var myCharSize: Int
  var isBind: Boolean = false
  var isSelected: Boolean = false
  var isDrawTopBorder: Boolean = false
  var isDrawBottomBorder: Boolean = false

  init {
    font = JBUI.Fonts.create(Font.MONOSPACED, 12).asBold()
    myCharSize = getFontMetrics(font).charWidth('m')
    border = BorderFactory.createEmptyBorder(
        2.scale(),
        (RADIUS / 2).scale(),
        2.scale(),
        (RADIUS / 2).scale()
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
      color = when {
        isSelected -> SELECTED_COLOR
        isBind -> SplitterPainter.FILLER_COLOR
        else -> BACKGROUND
      }
      fillRect(0, 0, width, height)
      with(create()) {
        if (!isSelected) {
          color = SplitterPainter.BORDER_COLOR over UIUtil.getPanelBackground()
          stroke = BasicStroke(JBUI.scale(1.0f))
          if (isDrawTopBorder) {
            drawLine(0, 0, width, 0)
          }
          if (isDrawBottomBorder) {
            drawLine(0, height - 1, width, height - 1)
          }
        }
      }
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
      color = TEXT_COLOR
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
    private var TEXT_COLOR_BRIGHT: Color = Color(0, 150, 0)
    private var TEXT_COLOR_DARK: Color = Color(106, 135, 89)
    val TEXT_COLOR by Settings.Theme(bright = TEXT_COLOR_BRIGHT, dark = TEXT_COLOR_DARK)

    private val BACKGROUND_BRIGHT: Color = Color.WHITE
    private val BACKGROUND_DARK: Color = Color(43, 43, 43)
    val BACKGROUND by Settings.Theme(dark = BACKGROUND_DARK, bright = BACKGROUND_BRIGHT)

    private val SELECTED_COLOR_BRIGHT: Color = Color(0, 100, 255, 30)
    private val SELECTED_COLOR_DARK: Color = Color(0, 100, 255, 30)
    val SELECTED_COLOR by Settings.Theme(dark = SELECTED_COLOR_DARK, bright = SELECTED_COLOR_BRIGHT)

    private const val RADIUS: Int = 10
  }
}