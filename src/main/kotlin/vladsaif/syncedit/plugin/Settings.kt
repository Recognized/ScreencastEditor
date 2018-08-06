package vladsaif.syncedit.plugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.util.ui.JBUI
import java.awt.Color
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

@State(name = "ScreencastEditorSettings")
data class Settings(
    private val rootMeanSquareColorBrightTheme: Int = Color(100, 100, 220).rgb,
    private val peakColorBrightTheme: Int = Color(50, 50, 200).rgb,
    private val wordSeparatorColorBrightTheme: Int = Color(150, 15, 160).rgb,
    private val wordColorBrightTheme: Int = Color(150, 15, 160).rgb,
    private val selectedRangeColorBrightTheme: Int = Color(200, 200, 200).rgb,
    private val playLineColorBrightTheme: Int = Color(0, 0, 0).rgb,

    private val rootMeanSquareColorDarcula: Int = Color(184, 244, 139).rgb,
    private val peakColorDarcula: Int = Color(96, 240, 112).rgb,
    private val wordSeparatorColorDarcula: Int = Color(255, 164, 160).rgb,
    private val wordColorDarcula: Int = Color(255, 164, 160).rgb,
    private val selectedRangeColorDarcula: Int = Color(123, 123, 123).rgb,
    private val playLineColorDarcula: Int = Color(220, 220, 220).rgb,

    private val wordSeparatorWidthDp: Float = 1.0f,
    private val peakStrokeWidthDp: Float = 1.0f,
    private val rootMeanSquareStrokeWidthDp: Float = 1.0f,
    private val dashWidthDp: Float = 10f
) : PersistentStateComponent<Settings> {

  val rootMeanSquareColor by Theme(bright = rootMeanSquareColorBrightTheme, dark = rootMeanSquareColorDarcula)
  val rootMeanSquareCutColor by Theme(bright = Color(rootMeanSquareColorBrightTheme).muchBrighter().rgb,
      dark = Color(rootMeanSquareColorDarcula).muchDarker().rgb)
  val peakColor by Theme(bright = peakColorBrightTheme, dark = peakColorDarcula)
  val peakCutColor by Theme(bright = Color(peakColorBrightTheme).muchBrighter().rgb,
      dark = Color(peakColorDarcula).muchDarker().rgb)
  val wordSeparatorColor by Theme(bright = wordSeparatorColorBrightTheme, dark = wordSeparatorColorDarcula)
  val wordColor by Theme(bright = wordColorBrightTheme, dark = wordColorDarcula)
  val selectionColor by Theme(bright = selectedRangeColorBrightTheme, dark = selectedRangeColorDarcula)
  val playLineColor by Theme(bright = playLineColorBrightTheme, dark = playLineColorDarcula)

  val wordSeparatorWidth by DpConverter(this::wordSeparatorWidthDp)
  val peakStrokeWidth by DpConverter(this::peakStrokeWidthDp)
  val rootMeanSquareStrokeWidth by DpConverter(this::rootMeanSquareStrokeWidthDp)
  val dashWidth by DpConverter(this::dashWidthDp)

  override fun getState(): Settings? = currentSettings

  override fun loadState(state: Settings) {
    currentSettings = state
  }

  companion object {
    @Volatile
    var currentSettings = Settings()

    private fun Color.muchBrighter() = this.brighter().brighter().brighter().brighter()

    private fun Color.muchDarker() = this.darker().darker().darker().darker()
  }

  class DpConverter(private val delegate: KProperty0<Float>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float = JBUI.scale(delegate.get())
  }

  class Theme(private val dark: Int, private val bright: Int) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Color {
      return if (com.intellij.util.ui.UIUtil.isUnderDarcula()) Color(dark)
      else Color(bright)
    }
  }
}

