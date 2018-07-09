package vladsaif.syncedit.plugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.ui.UIUtil
import java.awt.Color

data class Settings(
        private val rootMeanSquareColorBrightTheme: Int = Color(100, 100, 220).rgb,
        private val peakColorBrightTheme: Int = Color(50, 50, 200).rgb,
        private val wordSeparatorColorBrightTheme: Int = Color(150, 15, 160).rgb,
        private val wordColorBrightTheme: Int = Color(150, 15, 160).rgb,
        private val selectedRangeColorBrightTheme: Int = Color(200, 200, 200).rgb,

        private val rootMeanSquareColorDarcula: Int = Color(184, 244, 139).rgb,
        private val peakColorDarcula: Int = Color(96, 240, 112).rgb,
        private val wordSeparatorColorDarcula: Int = Color(255, 164, 160).rgb,
        private val wordColorDarcula: Int = Color(255, 164, 160).rgb,
        private val selectedRangeColorDacrula: Int = Color(123, 123, 123).rgb,

        val wordSeparatorWidth: Float = 2.0f,
        val peakStrokeWidth: Float = 1.2f,
        val rootMeanSquareStrokeWidth: Float = 1.0f
        ) : PersistentStateComponent<Settings> {

    val rootMeanSquareColor
        get() = if (!UIUtil.isUnderDarcula()) Color(rootMeanSquareColorBrightTheme)
        else Color(rootMeanSquareColorDarcula)

    val peakColor
        get() = if (!UIUtil.isUnderDarcula()) Color(peakColorBrightTheme)
        else Color(peakColorDarcula)

    val wordSeparatorColor
        get() = if (!UIUtil.isUnderDarcula()) Color(wordSeparatorColorBrightTheme)
        else Color(wordSeparatorColorDarcula)

    val wordColor
        get() = if (!UIUtil.isUnderDarcula()) Color(wordColorBrightTheme)
        else Color(wordColorDarcula)

    val selectionColor
        get() = if (!UIUtil.isUnderDarcula()) Color(selectedRangeColorBrightTheme)
        else Color(selectedRangeColorDacrula)

    override fun getState(): Settings? = currentSettings

    override fun loadState(state: Settings) {
        currentSettings = state
    }

    companion object {
        @Volatile
        var currentSettings = Settings()
    }
}