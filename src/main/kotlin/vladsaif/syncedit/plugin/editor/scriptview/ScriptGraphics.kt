package vladsaif.syncedit.plugin.editor.scriptview

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.audioview.WaveformGraphics
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font


object ScriptGraphics {
  val FONT_COLOR: Color get() = WaveformGraphics.WORD_COLOR
  val FONT: Font get() = UIUtil.getLabelFont()
  val CODE_BLOCK_BACKGROUND = Color(225, 249, 225) to Color(52, 78, 57)
  val CODE_BLOCK_BORDER: Color = WaveformGraphics.WORD_SEPARATOR_COLOR
  val BORDER_WIDTH = WaveformGraphics.WORD_SEPARATOR_WIDTH
  val BORDER_STROKE = BasicStroke(
    WaveformGraphics.WORD_SEPARATOR_WIDTH,
    BasicStroke.CAP_BUTT,
    BasicStroke.JOIN_BEVEL,
    0f,
    FloatArray(1) { WaveformGraphics.DASH_WIDTH },
    0f
  )
  val PADDING = JBUI.scale(4.0f)
  val STROKE_WIDTH = JBUI.scale(1.0f)
  val BIG_MARK_HEIGHT = JBUI.scale(3.0f)
  val SMALL_MARK_HEIGHT = JBUI.scale(1.5f)
  val BORDER_PRECISION = JBUI.scale(2)

  private val TEMP_BUFFER = FloatArray(3)
  fun rotate(it: Color): Color {
    val values = Color.RGBtoHSB(it.red, it.green, it.blue, TEMP_BUFFER)
    values[0] = (values[0] + 0.20f) % 1.0f
    val (h, s, v) = values
    return Color(Color.HSBtoRGB(h, s, v))
  }

}
