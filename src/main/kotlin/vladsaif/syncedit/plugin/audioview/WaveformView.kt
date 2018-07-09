package vladsaif.syncedit.plugin.audioview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollBar
import com.intellij.ui.components.JBScrollPane
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.Settings
import vladsaif.syncedit.plugin.ClosedLongRange
import java.awt.*
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.ScrollPaneConstants
import kotlin.math.max
import kotlin.math.min

class WaveformView(sampleProvider: SampleProvider) : JBScrollPane(UnderlyingPanel(sampleProvider)) {
    private val panel = viewport.view as UnderlyingPanel

    init {
        background = Color.WHITE
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
        horizontalScrollBar.addAdjustmentListener lit@{ _ ->
            panel.extent = this@WaveformView.viewport.width.toLong()
            panel.position = horizontalScrollBar.value.toLong()
            panel.repaint()
        }
    }

    fun zoomOut() {
        panel.zoomOut()
        resetScrollBarValues()
    }

    fun zoomIn(zoomPosition: Long = panel.position + panel.extent / 2) {
        panel.zoomIn(zoomPosition)
        resetScrollBarValues()
    }

    private fun resetScrollBarValues() {
        with(panel) {
            val jbBar = horizontalScrollBar as JBScrollBar
            jbBar.valueIsAdjusting = true
            jbBar.maximum = availableChunks.toInt() - 1
            jbBar.setCurrentValue(position.toInt())
            if (position.toInt() != jbBar.value) println("JBScrollbar ignored set value")
            jbBar.valueIsAdjusting = false
        }
    }

    private class UnderlyingPanel(private val sampleProvider: SampleProvider) : JBPanel<UnderlyingPanel>() {
        private val visibleRange
            get() = ClosedLongRange(position, chunkEnd)
        private var cachedRange: ClosedLongRange? = null
        private var dataCache: List<AveragedSampleData>? = null
        private val chunkEnd
            get() = position + extent - 1
        private val drawRange
            get() = ClosedLongRange(max((position - extent * 2), 0), min((chunkEnd + extent * 2), availableChunks - 1))
        var availableChunks = Toolkit.getDefaultToolkit().screenSize.width.toLong()
            set(value) {
                field = minOf(max(value, sampleProvider.totalFrames / maxSamplesPerChunk),
                        sampleProvider.totalFrames / minSamplesPerChunk,
                        Int.MAX_VALUE / minSamplesPerChunk)
            }
        var position = 0L
            set(value) {
                field = min(availableChunks - extent, max(value, 0))
            }
        var extent = availableChunks

        init {
            background = Color.WHITE
        }

        fun zoomOut() {
            availableChunks /= 2
            position /= 2
            cleanCache()
        }

        fun zoomIn(zoomPosition: Long) {
            position = zoomPosition - extent / 4
            availableChunks *= 2
            position *= 2
            cleanCache()
        }

        private fun cleanCache() {
            dataCache = null
            cachedRange = null
        }

        override fun getPreferredSize() = Dimension(availableChunks.toInt(), parent.height)

        override fun paintComponent(graphics: Graphics?) {
            println(DecodedMpegAudioInputStream::class)
            super.paintComponent(graphics)
            graphics ?: return
            val graphics2d = graphics as Graphics2D
            graphics2d.clearRect(0, 0, width, height)
            graphics2d.drawHorizontalLine()
            synchronized(this) {
                if (cachedRange?.intersects(visibleRange) == true) {
                    graphics2d.drawAveragedWaveform(dataCache!![0])
                } else {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        sampleProvider.getAveragedSampleData(availableChunks, drawRange).also {
                            synchronized(this@UnderlyingPanel) {
                                dataCache = it
                                cachedRange = visibleRange
                            }
                            ApplicationManager.getApplication().invokeLater() { this@UnderlyingPanel.repaint() }
                        }
                    }
                }
            }
        }

        private fun Graphics2D.drawHorizontalLine() {
            color = Color.BLACK
            drawLine(drawRange.start.toInt(), height / 2, drawRange.end.toInt(), height / 2)
        }

        private fun Graphics2D.drawAveragedWaveform(data: AveragedSampleData) {
            color = Settings.peakColor
            stroke = BasicStroke(Settings.peakStrokeWidth)
            for (i in 0 until data.size) {
                val yTop = (height - (data.highestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                val yBottom = (height - (data.lowestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine((i + data.skippedChunks).toInt(), yTop.toInt(), (i + data.skippedChunks).toInt(), yBottom.toInt())
            }
            stroke = BasicStroke(Settings.rootMeanSquareStrokeWidth)
            color = Settings.rootMeanSquareColor
            for (i in 0 until data.size) {
                val rmsHeight = (data.rootMeanSquare[i] * height).toDouble() / data.maxPeak / 4
                val yAverage = (height - (data.averagePeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine((i + data.skippedChunks).toInt(), (yAverage - rmsHeight).toInt(), (i + data.skippedChunks).toInt(), (yAverage + rmsHeight).toInt())
            }
        }

        private fun LongArray.averageN(n: Int): LongArray {
            val ret = LongArray(size)
            var sum = 0L
            for (i in -n until size + n) {
                if (i + n in 0..(size - 1)) {
                    sum += this[i + n]
                }
                if (i - n - 1 in 0..(size - 1)) {
                    sum -= this[i - n - 1]
                }
                if (i in 0..(size - 1)) {
                    ret[i] = sum / (n * 2 + 1)
                }
            }
            return ret
        }
    }

    companion object {
        private const val maxSamplesPerChunk = 100000L
        private const val minSamplesPerChunk = 20L
    }
}


fun main(args: Array<String>) {
    JFrame("Hello").apply {
        add(WaveformView(BasicSampleProvider(Paths.get("nocturne.mp3"))).also {
            isVisible = true
        })
        size = Dimension(1000, 400)
        isVisible = true
        preferredSize = Dimension(1000, 400)
    }
}