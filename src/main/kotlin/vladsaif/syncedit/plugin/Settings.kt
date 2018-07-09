package vladsaif.syncedit.plugin

import com.intellij.util.ui.UIUtil
import java.awt.Color

object Settings {
    private val rootMeanSquareColorBrightTheme = Color(100, 100, 220)

    private val peakColorBrightTheme = Color(50, 50, 200)

    private val rootMeanSquareColorDarcula = Color(200, 200, 230)

    private val peakColorDarcula = Color(150, 150, 255)

    val rootMeanSquareColor
        get() = if (!UIUtil.isUnderDarcula()) rootMeanSquareColorBrightTheme
        else rootMeanSquareColorDarcula

    val peakColor
        get() = if (!UIUtil.isUnderDarcula()) peakColorBrightTheme
        else peakColorDarcula

    val peakStrokeWidth
        get() = 1.2f

    val rootMeanSquareStrokeWidth
        get() = 1.0f


}