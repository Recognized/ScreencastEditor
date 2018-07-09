package vladsaif.syncedit.plugin.audioview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollBar
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream
import vladsaif.syncedit.plugin.Alias
import vladsaif.syncedit.plugin.Settings
import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.Word
import java.awt.*
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.ScrollPaneConstants
import kotlin.math.max
import kotlin.math.min

class WaveformView(sampleProvider: SampleProvider) : JBScrollPane(UnderlyingPanel(sampleProvider)) {
    private val panel = viewport.view as UnderlyingPanel
    var wordData by Alias(panel::wordData)

    init {
        background = Color.WHITE
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
        horizontalScrollBar.addAdjustmentListener { _ ->
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
        private val wordFont
            get() = JBUI.Fonts.miniFont()
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
        var wordData = listOf<Word>()

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
            graphics2d.drawWords()
        }

        private fun Graphics2D.drawHorizontalLine() {
            color = Color.BLACK
            drawLine(drawRange.start.toInt(), height / 2, drawRange.end.toInt(), height / 2)
        }

        private fun Graphics2D.drawAveragedWaveform(data: AveragedSampleData) {
            color = Settings.currentSettings.peakColor
            stroke = BasicStroke(Settings.currentSettings.peakStrokeWidth)
            for (i in 0 until data.size) {
                val yTop = (height - (data.highestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                val yBottom = (height - (data.lowestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine((i + data.skippedChunks).toInt(), yTop.toInt(), (i + data.skippedChunks).toInt(), yBottom.toInt())
            }
            stroke = BasicStroke(Settings.currentSettings.rootMeanSquareStrokeWidth)
            color = Settings.currentSettings.rootMeanSquareColor
            for (i in 0 until data.size) {
                val rmsHeight = (data.rootMeanSquare[i] * height).toDouble() / data.maxPeak / 4
                val yAverage = (height - (data.averagePeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine((i + data.skippedChunks).toInt(), (yAverage - rmsHeight).toInt(), (i + data.skippedChunks).toInt(), (yAverage + rmsHeight).toInt())
            }
        }

        private fun Graphics2D.drawWords() {
            val usedRange = drawRange
            wordData.forEach {
                val leftBound = it.timeStart.toXCoordinate()
                val rightBound = it.timeEnd.toXCoordinate()
                if (ClosedLongRange(leftBound.toLong(), rightBound.toLong()).intersects(usedRange)) {
                    drawCenteredWord(it.text, leftBound, rightBound)
                }
                color = Settings.currentSettings.wordSeparatorColor
                stroke = BasicStroke(Settings.currentSettings.wordSeparatorWidth,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,
                        0f,
                        FloatArray(1) { 10.0f },
                        0f)
                if (leftBound > usedRange.start) {
                    drawLine(leftBound, 0, leftBound, height)
                }
                if (rightBound < usedRange.end) {
                    drawLine(rightBound, 0, rightBound, height)
                }
            }
        }

        // x1 <= x2
        private fun Graphics2D.drawCenteredWord(word: String, x1: Int, x2: Int) {
            if (x2 < x1) throw IllegalArgumentException()
            val stringWidth = getFontMetrics(wordFont).stringWidth(word)
            if (stringWidth < x2 - x1) {
                val pos = (x2 + x1 - stringWidth) / 2
                color = Settings.currentSettings.wordColor
                font = UIUtil.getFont(UIUtil.FontSize.MINI, null)
                drawString(word, pos, height / 6)
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

        private fun TimeMillis.toXCoordinate() =
                sampleProvider.getChunkOfFrame(availableChunks, (this / sampleProvider.millisecondsPerFrame).toLong())
    }

    companion object {
        private const val maxSamplesPerChunk = 100000L
        private const val minSamplesPerChunk = 20L
    }
}


fun main(args: Array<String>) {
    JFrame("Hello").apply {
        add(WaveformView(BasicSampleProvider(Paths.get("untitled2.wav"))).also {
            isVisible = true
        })
        size = Dimension(1000, 400)
        isVisible = true
        preferredSize = Dimension(1000, 400)
    }
}