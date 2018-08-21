package vladsaif.syncedit.plugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.util.ui.JBUI
import java.awt.Color
import kotlin.reflect.KProperty

@State(name = "ScreencastEditorSettings")
object Settings : PersistentStateComponent<Settings.State> {

  private var STATE = State()

  val ROOT_MEAN_SQUARE_COLOR by Theme(
      bright = STATE.rootMeanSquareColorBrightTheme,
      dark = STATE.rootMeanSquareColorDarcula
  )
  val ROOT_MEAN_SQUARE_CUT_COLOR by Theme(
      bright = Color(STATE.rootMeanSquareColorBrightTheme).muchBrighter().rgb,
      dark = Color(STATE.rootMeanSquareColorDarcula).muchDarker().rgb
  )
  val PEAK_COLOR by Theme(
      bright = STATE.peakColorBrightTheme,
      dark = STATE.peakColorDarcula
  )
  val PEAK_CUT_COLOR by Theme(
      bright = Color(STATE.peakColorBrightTheme).muchBrighter().rgb,
      dark = Color(STATE.peakColorDarcula).muchDarker().rgb
  )
  val WORD_SEPARATOR_COLOR by Theme(
      bright = STATE.wordSeparatorColorBrightTheme,
      dark = STATE.wordSeparatorColorDarcula
  )
  val WORD_MOVING_SEPARATOR_COLOR by Theme(
      bright = STATE.wordMovingSeparatorColorBrightTheme,
      dark = STATE.wordMovingSeparatorColorDarcula
  )
  val WORD_COLOR by Theme(
      bright = STATE.wordColorBrightTheme,
      dark = STATE.wordColorDarcula
  )
  val SELECTION_COLOR by Theme(
      bright = STATE.selectedRangeColorBrightTheme,
      dark = STATE.selectedRangeColorDarcula
  )

  val PLAY_LINE_COLOR by Theme(
      bright = STATE.playLineColorBrightTheme,
      dark = STATE.playLineColorDarcula
  )

  private var TEXT_COLOR_BRIGHT: Color = Color(0, 150, 0)
  private var TEXT_COLOR_DARK: Color = Color(106, 135, 89)
  val DIFF_TEXT_COLOR by Settings.Theme(bright = TEXT_COLOR_BRIGHT, dark = TEXT_COLOR_DARK)

  private val BACKGROUND_BRIGHT: Color = Color.WHITE
  private val BACKGROUND_DARK: Color = Color(43, 43, 43)
  val DIFF_BACKGROUND by Settings.Theme(dark = BACKGROUND_DARK, bright = BACKGROUND_BRIGHT)

  private val SELECTED_COLOR_BRIGHT: Color = Color(225, 237, 255)
  private val SELECTED_COLOR_DARK: Color = BACKGROUND_DARK.brighter().brighter()
  val DIFF_SELECTED_COLOR by Settings.Theme(dark = SELECTED_COLOR_DARK, bright = SELECTED_COLOR_BRIGHT)

  private val FILLER_COLOR_BRIGHT: Color = Color(225, 249, 225)
  private val FILLER_COLOR_DARK: Color = Color(52, 78, 57)
  private val BORDER_COLOR_BRIGHT: Color = Color(185, 240, 185)
  private val BORDER_COLOR_DARK: Color = Color(43, 99, 47)
  val DIFF_FILLER_COLOR by Settings.Theme(bright = FILLER_COLOR_BRIGHT, dark = FILLER_COLOR_DARK)
  val DIFF_BORDER_COLOR by Settings.Theme(bright = BORDER_COLOR_BRIGHT, dark = BORDER_COLOR_DARK)
  private val DIFF_HOVERED_COLOR_BRIGHT: Color = Color(234, 252, 255)
  val DIFF_HOVERED_COLOR by Settings.Theme(
      bright = DIFF_HOVERED_COLOR_BRIGHT,
      dark = BACKGROUND_DARK.brighter()
  )

  val WORD_SEPARATOR_WIDTH get() = JBUI.scale(STATE.wordSeparatorWidthDp)
  val PEAK_STROKE_WIDTH get() = JBUI.scale(STATE.peakStrokeWidthDp)
  val ROOT_MEAN_SQUARE_STROKE_WIDTH get() = JBUI.scale(STATE.rootMeanSquareStrokeWidthDp)
  val DASH_WIDTH get() = JBUI.scale(STATE.dashWidthDp)

  class Theme(private val dark: Color, private val bright: Color) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Color {
      return if (com.intellij.util.ui.UIUtil.isUnderDarcula()) dark
      else bright
    }

    constructor(dark: Int, bright: Int) : this(dark = Color(dark), bright = Color(bright))
  }

  class State {
    var rootMeanSquareColorBrightTheme: Int = Color(100, 100, 220).rgb
    var peakColorBrightTheme: Int = Color(50, 50, 200).rgb
    var wordSeparatorColorBrightTheme: Int = Color(150, 15, 160).rgb
    var wordColorBrightTheme: Int = Color(150, 15, 160).rgb
    var selectedRangeColorBrightTheme: Int = Color(200, 200, 200).rgb
    var playLineColorBrightTheme: Int = Color(0, 0, 0).rgb
    var wordMovingSeparatorColorBrightTheme: Int = Color(60, 60, 60).rgb
    var rootMeanSquareColorDarcula: Int = Color(255, 176, 71).rgb
    var peakColorDarcula: Int = Color(255, 146, 31).rgb
    var wordSeparatorColorDarcula: Int = Color(255, 164, 160).rgb
    var wordColorDarcula: Int = Color(255, 164, 160).rgb
    var selectedRangeColorDarcula: Int = Color(123, 123, 123).rgb
    var playLineColorDarcula: Int = Color(220, 220, 220).rgb
    var wordMovingSeparatorColorDarcula: Int = Color(170, 70, 30).rgb
    var wordSeparatorWidthDp: Float = 1.0f
    var peakStrokeWidthDp: Float = 1.0f
    var rootMeanSquareStrokeWidthDp: Float = 1.0f
    var dashWidthDp: Float = 10f
  }

  override fun getState(): State? = STATE

  override fun loadState(state: State) {
    STATE = state
  }

  private fun Color.muchBrighter() = this.brighter().brighter().brighter().brighter()

  private fun Color.muchDarker() = this.darker().darker().darker().darker()

}
