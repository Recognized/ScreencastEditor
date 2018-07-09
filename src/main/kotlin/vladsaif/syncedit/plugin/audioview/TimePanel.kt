package vladsaif.syncedit.plugin.audioview

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Graphics

class TimePanel(timeStartMs: Double, timeEndMs: Double) : JBPanel<TimePanel>() {
    val length
        get() = timeEndMs - timeStartMs
    var timeStartMs = timeStartMs
        set(value) {
            if (value > timeEndMs) {
                throw IllegalArgumentException("Start time offset cannot be greater than end time offset")
            }
            field = value
        }
    var timeEndMs = timeEndMs
        set(value) {
            if (value < timeStartMs) {
                throw IllegalArgumentException("End time offset cannot be less than start time offset")
            }
            field = value
        }
    val granularity = 0
//    get() =


    override fun getPreferredSize(): Dimension {
        return Dimension(parent.width, adequateHeight)
    }

    override fun paintComponent(g: Graphics?) {

    }

    companion object {
        private val adequateHeight
            get() = (UIUtil.getFontSize(UIUtil.FontSize.NORMAL) * 1.5).toInt()
    }
}