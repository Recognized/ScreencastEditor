package vladsaif.syncedit.plugin

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.audioview.BasicStatProvider
import vladsaif.syncedit.plugin.audioview.WaveformView
import java.awt.Color
import java.awt.Dimension
import java.nio.file.Paths
import javax.swing.JFrame
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

fun main(args: Array<String>) {
    JFrame("Hello").apply {
        add(WaveformView(BasicStatProvider(Paths.get("untitled2.wav"))).also {
            isVisible = true
        })
        size = Dimension(1000, 400)
        isVisible = true
        preferredSize = Dimension(1000, 400)
    }
}

fun Long.floorToInt(): Int {
    return if (this > 0) min(this, Int.MAX_VALUE.toLong()).toInt()
    else max(this, Int.MIN_VALUE.toLong()).toInt()
}


class Alias<T>(private val delegate: KMutableProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            delegate.get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        delegate.set(value)
    }
}

class DpConverter(private val delegate: KProperty0<Float>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float = JBUI.scale(delegate.get())
}

class Theme(private val dark: Int, private val bright: Int) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Color {
        return if (UIUtil.isUnderDarcula()) Color(dark)
        else Color(bright)
    }
}