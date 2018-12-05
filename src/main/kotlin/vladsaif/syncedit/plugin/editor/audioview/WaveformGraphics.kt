package vladsaif.syncedit.plugin.editor.audioview

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color

object WaveformGraphics {
  private var STATE = ColorState()
  private val LOG = logger<WaveformGraphics>()

  init {
    LOG.info(
      """
      User graphics info:
        pixScale = ${JBUI.pixScale()}
        pixScale(1) = ${JBUI.pixScale(1.0f)}
        scale(1) = ${JBUI.scale(1)}
        sysScale = ${JBUI.sysScale()}
        isUserHiDPI = ${JBUI.isUsrHiDPI()}
        scaleFontSize(12.0f) = ${JBUI.scaleFontSize(12.0f)}
    """.trimIndent()
    )
  }

  val AUDIO_PEAK_COLOR = STATE.audioPeak.toJBColor()
  val AUDIO_PLAY_LINE_COLOR = STATE.audioPlayLine.toJBColor()
  val AUDIO_SELECTION_COLOR = STATE.audioSelectedRange.toJBColor()
  val AUDIO_RMS_COLOR = STATE.audioRms.toJBColor()
  val HORIZONTAL_LINE = JBColor(Color.BLACK, AUDIO_PEAK_COLOR)
  val MAPPING_HIGHLIGHT_COLOR = STATE.mappingHighlight.toJBColor()
  val WORD_COLOR = STATE.word.toJBColor()
  val WORD_MOVING_SEPARATOR_COLOR = STATE.wordMovingSeparator.toJBColor()
  val WORD_SEPARATOR_COLOR = STATE.wordSeparator.toJBColor()
  val OUTER_GLOW = STATE.outerGlow.toJBColor()

  val DASH_WIDTH get() = JBUI.scale(STATE.dashWidthDp)
  val WORD_SEPARATOR_WIDTH get() = JBUI.scale(STATE.wordSeparatorWidthDp)
  val OUTER_GLOW_WIDTH get() = JBUI.scale(STATE.outerGlowWidthDp)

  private infix fun Color.or(other: Color): Long {
    return (this to other).toLong()
  }

  private fun Pair<Color, Color>.toLong(): Long {
    val (bright, dark) = this
    return bright.rgb.toLong() + (dark.rgb.toLong() shl 32)
  }

  private fun Long.toJBColor(): JBColor {
    return JBColor(getBright(), getDark())
  }

  private fun Long.getDark(): Color {
    return Color((this ushr 32).toInt(), true)
  }

  private fun Long.getBright(): Color {
    return Color((this and ((1L shl 32) - 1)).toInt(), true)
  }

  private fun Color.muchBrighter() = this.brighter().brighter().brighter().brighter()

  private fun Color.muchDarker() = this.darker().darker().darker().darker()

  @State(name = "ScreencastEditorGraphics", storages = [Storage(file = "screencastEditorGraphics.xml")])
  class ColorState : PersistentStateComponent<ColorState> {
    var mappingHighlight = Color(225, 249, 225) or Color(52, 78, 57)
    var audioPlayLine = Color(0, 0, 0) or Color(220, 220, 220)
    var audioPeak = Color(50, 50, 200) or Color(255, 146, 31)
    var audioSelectedRange = Color(200, 200, 200) or Color(123, 123, 123)
    var audioRms = Color(100, 100, 220) or Color(255, 176, 71)
    var word = Color(150, 15, 160) or Color(255, 164, 160)
    var wordSeparator = Color(150, 15, 160) or Color(255, 164, 160)
    var wordMovingSeparator = Color(60, 60, 60) or Color(170, 70, 30)
    var outerGlow = Color(255, 255, 100) or Color(200, 100, 70)

    var dashWidthDp: Float = 10f
    var wordSeparatorWidthDp: Float = 1.0f
    var outerGlowWidthDp: Float = 2.0f

    override fun getState(): ColorState? {
      LOG.info("Getting state")
      return STATE
    }

    override fun noStateLoaded() {
      LOG.info("No state loaded...")
    }

    override fun loadState(state: ColorState) {
      LOG.info("Loading state...")
      STATE = state
    }
  }
}
