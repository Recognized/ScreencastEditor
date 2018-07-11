package vladsaif.syncedit.plugin

import vladsaif.syncedit.plugin.audioview.BasicStatProvider
import vladsaif.syncedit.plugin.audioview.WaveformView
import java.awt.Dimension
import java.nio.file.Paths
import javax.swing.JFrame
import kotlin.math.max
import kotlin.math.min

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