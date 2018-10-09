package vladsaif.syncedit.plugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color

@State(name = "ScreencastEditorSettings")
object ColorSettings : PersistentStateComponent<ColorSettings.ColorState> {
  private var STATE = ColorState()

  val AUDIO_PEAK_COLOR get() = STATE.audioPeak.toJBColor()
  val AUDIO_PEAK_CUT_COLOR
    get() = JBColor(STATE.audioPeak.getBright().muchBrighter(), STATE.audioPeak.getDark().muchDarker())
  val AUDIO_PLAY_LINE_COLOR get() = STATE.audioPlayLine.toJBColor()
  val AUDIO_SELECTION_COLOR get() = STATE.audioSelectedRange.toJBColor()
  val AUDIO_RMS_CUT_COLOR
    get() = JBColor(STATE.audioRms.getBright().muchBrighter(), STATE.audioRms.getDark().muchBrighter().muchDarker())
  val AUDIO_RMS_COLOR get() = STATE.audioRms.toJBColor()
  val MAPPING_TEXT_COLOR get() = STATE.mappingText.toJBColor()
  val MAPPING_BACKGROUND get() = STATE.mappingBackground.toJBColor()
  val MAPPING_SELECTION_COLOR get() = STATE.mappingSelection.toJBColor()
  val MAPPING_HIGHLIGHT_COLOR get() = STATE.mappingHighlight.toJBColor()
  val MAPPING_BORDER_COLOR get() = STATE.mappingBorder.toJBColor()
  val MAPPING_HOVERED_COLOR get() = STATE.mappingHovered.toJBColor()
  val WORD_COLOR get() = STATE.word.toJBColor()
  val WORD_MOVING_SEPARATOR_COLOR get() = STATE.wordMovingSeparator.toJBColor()
  val WORD_SEPARATOR_COLOR get() = STATE.wordSeparator.toJBColor()

  val DASH_WIDTH get() = JBUI.scale(STATE.dashWidthDp)
  val PEAK_STROKE_WIDTH get() = JBUI.scale(STATE.peakStrokeWidthDp)
  val ROOT_MEAN_SQUARE_STROKE_WIDTH get() = JBUI.scale(STATE.rootMeanSquareStrokeWidthDp)
  val WORD_SEPARATOR_WIDTH get() = JBUI.scale(STATE.wordSeparatorWidthDp)

  class ColorState {
    var mappingText = Color(0, 150, 0) or Color(106, 135, 89)
    var mappingHighlight = Color(225, 249, 225) or Color(52, 78, 57)
    var mappingBorder = Color(185, 240, 185) or Color(43, 99, 47)
    var mappingBackground = Color.WHITE or Color(43, 43, 43)
    var mappingSelection = Color(225, 237, 255) or mappingBackground.getDark().brighter().brighter()
    var mappingHovered = Color(234, 252, 255) or mappingBackground.getDark().brighter()

    var audioPlayLine = Color(0, 0, 0) or Color(220, 220, 220)
    var audioPeak = Color(50, 50, 200) or Color(255, 146, 31)
    var audioSelectedRange = Color(200, 200, 200) or Color(123, 123, 123)
    var audioRms = Color(100, 100, 220) or Color(255, 176, 71)
    var word = Color(150, 15, 160) or Color(255, 164, 160)
    var wordSeparator = Color(150, 15, 160) or Color(255, 164, 160)
    var wordMovingSeparator = Color(60, 60, 60) or Color(170, 70, 30)

    var dashWidthDp: Float = 10f
    var peakStrokeWidthDp: Float = 1.0f
    var rootMeanSquareStrokeWidthDp: Float = 1.0f
    var wordSeparatorWidthDp: Float = 1.0f
  }

  private infix fun Color.or(other: Color): Long {
    return (this to other).toLong()
  }

  private fun Pair<Color, Color>.toLong(): Long {
    val (bright, dark) = this
    return bright.rgb.toLong() or (dark.rgb.toLong() shl 32)
  }

  private fun Long.toJBColor(): JBColor {
    return JBColor(getBright(), getDark())
  }

  private fun Long.getDark(): Color {
    return Color((this ushr 32).toInt())
  }

  private fun Long.getBright(): Color {
    return Color((this and ((1 shl 32) - 1)).toInt())
  }

  override fun getState(): ColorState? = STATE

  override fun loadState(state: ColorState) {
    STATE = state
  }

  private fun Color.muchBrighter() = this.brighter().brighter().brighter().brighter()

  private fun Color.muchDarker() = this.darker().darker().darker().darker()

  private infix fun Color.over(other: Color): Color {
    assert(other.alpha == 255)
    val srcRed = this.red * this.alpha / 255
    val srcGreen = this.green * this.alpha / 255
    val srcBlue = this.blue * this.alpha / 255
    val dstRed = other.red * (255 - this.alpha) / 255
    val dstGreen = other.green * (255 - this.alpha) / 255
    val dstBlue = other.blue * (255 - this.alpha) / 255
    return Color(srcRed + dstRed, srcGreen + dstGreen, srcBlue + dstBlue)
  }
}
