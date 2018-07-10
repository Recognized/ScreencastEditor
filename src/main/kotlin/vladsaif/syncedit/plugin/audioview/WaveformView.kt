package vladsaif.syncedit.plugin.audioview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollBar
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.nio.file.Paths
import javax.swing.BoundedRangeModel
import javax.swing.DefaultBoundedRangeModel
import javax.swing.JFrame
import javax.swing.ScrollPaneConstants
import kotlin.math.max
import kotlin.math.min

private val logger = logger<WaveformView>()
class WaveformView(statProvider: StatProvider) : JBScrollPane(UnderlyingPanel(statProvider)) {
    private val panel = viewport.view as UnderlyingPanel
    var wordData by Alias(panel::wordData)

    init {
        background = Color.WHITE
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
        horizontalScrollBar.addAdjustmentListener { _ ->
            panel.extent = this@WaveformView.viewport.width
            panel.value = horizontalScrollBar.value
            panel.repaint()
        }
    }

    fun zoomOut() {
        panel.zoomOut()
        updateScrollBarValues()
    }

    fun zoomIn() {
        panel.zoomIn()
        updateScrollBarValues()
    }

    private fun updateScrollBarValues() {
        with(panel) {
            val jbBar = horizontalScrollBar as JBScrollBar
            jbBar.valueIsAdjusting = true
            jbBar.maximum = maximum
            jbBar.setCurrentValue(value)
            if (value != jbBar.value) println("JBScrollbar ignored set value")
            jbBar.valueIsAdjusting = false
        }
    }

    private class UnderlyingPanel(
            private val statProvider: StatProvider,
            private val model: BoundedRangeModel = DefaultBoundedRangeModel()
    ) : JBPanel<UnderlyingPanel>(), BoundedRangeModel by model {
        private val visibleRange
            get() = ClosedIntRange(value, value + extent)
        private val wordFont
            get() = JBUI.Fonts.label()
        private val drawRange
            get() = ClosedIntRange(max(value - extent * 2, minimum), min(value + extent * 2, maximum))
        private var cachedRange: ClosedIntRange? = null
        private var dataCache: List<AveragedSampleData>? = null
        private val selectedRanges = ClosedIntRangeUnion()
        private var hoveredRange = ClosedIntRange.EMPTY_RANGE
        var wordData = listOf<Word>()

        val mouseMotionListener = object : MouseMotionListener {
            override fun mouseMoved(e: MouseEvent?) {
                e ?: return
                val previous = hoveredRange
                hoveredRange = getEnclosingWordRange(e.x)
                if (hoveredRange != previous) repaint()
                logger.info("HoveredRange=$hoveredRange")
            }

            override fun mouseDragged(e: MouseEvent?) = Unit
        }

        val mouseListener = object : MouseListener {
            override fun mouseReleased(e: MouseEvent?) {
                e ?: return
            }

            override fun mouseEntered(e: MouseEvent?) {
            }

            override fun mouseClicked(e: MouseEvent?) {
                e ?: return
                val rangeUnderClick = getEnclosingWordRange(e.x)
                logger.info("Clicked=$rangeUnderClick")
                if (rangeUnderClick.empty) return
                if (e.isControlDown) {
                    if (rangeUnderClick in selectedRanges) {
                        selectedRanges.union(rangeUnderClick)
                    } else {
                        selectedRanges.exclude(rangeUnderClick)
                    }
                } else {
                    selectedRanges.clear()
                    selectedRanges.union(rangeUnderClick)
                }
            }

            override fun mouseExited(e: MouseEvent?) {
            }

            override fun mousePressed(e: MouseEvent?) {
            }
        }

        init {
            maximum = Toolkit.getDefaultToolkit().screenSize.width - 1
            minimum = 0
            value = 0
            extent = maximum
            background = Color.WHITE
            addMouseListener(mouseListener)
            addMouseMotionListener(mouseMotionListener)
            logger.info("Model constraints: min=$minimum, value=$value, extent=$extent, max=$maximum")
        }


        override fun setMaximum(newMaximum: Int) {
            model.maximum = minOf(max(newMaximum, (statProvider.totalFrames / maxSamplesPerChunk).floorToInt()),
                    (statProvider.totalFrames / minSamplesPerChunk).floorToInt(),
                    Int.MAX_VALUE / minSamplesPerChunk)
        }

        private fun getEnclosingWordRange(coordinate: Int): ClosedIntRange {
            val enclosingWord = wordData.find { it.coordinates.contains(coordinate) }
            return enclosingWord?.coordinates ?: ClosedIntRange.EMPTY_RANGE
        }

        private fun invertSelection(range: ClosedIntRange) {

        }

        fun zoomOut() {
            maximum /= 2
            value /= 2
            cleanCache()
        }

        fun zoomIn(zoomPosition: Int = value + extent / 2) {
            value = zoomPosition - extent / 4
            maximum *= 2
            value *= 2
            cleanCache()
        }

        private fun cleanCache() {
            dataCache = null
            cachedRange = null
            hoveredRange = ClosedIntRange.EMPTY_RANGE
            selectedRanges.clear()
        }

        override fun getPreferredSize() = Dimension(maximum, parent.height)

        override fun paintComponent(graphics: Graphics?) {
            super.paintComponent(graphics)
            graphics ?: return
            val graphics2d = graphics as Graphics2D
            graphics2d.clearRect(0, 0, width, height)
            graphics2d.drawSelectedRanges()
            graphics2d.drawHorizontalLine()
            synchronized(this) {
                if (cachedRange?.intersects(visibleRange) == true) {
                    graphics2d.drawAveragedWaveform(dataCache!![0])
                } else {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        statProvider.getAveragedSampleData(maximum, drawRange).also {
                            synchronized(this@UnderlyingPanel) {
                                dataCache = it
                                cachedRange = visibleRange
                            }
                            ApplicationManager.getApplication().invokeLater { this@UnderlyingPanel.repaint() }
                        }
                    }
                }
            }
            graphics2d.drawWords()
        }

        private fun Graphics2D.drawSelectedRanges() {
            val hoveredVisible = drawRange.intersect(hoveredRange)
            if (!hoveredVisible.empty) {
                color = Settings.currentSettings.selectionColor
                fillRect(hoveredVisible.start, 0, hoveredVisible.length, height)
            }
        }

        private fun Graphics2D.drawHorizontalLine() {
            color = Color.BLACK
            drawLine(drawRange.start, height / 2, drawRange.end, height / 2)
        }

        private fun Graphics2D.drawAveragedWaveform(data: AveragedSampleData) {
            color = Settings.currentSettings.peakColor
            stroke = BasicStroke(Settings.currentSettings.peakStrokeWidth)
            for (i in 0 until data.size) {
                val yTop = (height - (data.highestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                val yBottom = (height - (data.lowestPeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine(i + data.skippedChunks, yTop.toInt(), i + data.skippedChunks, yBottom.toInt())
            }
            stroke = BasicStroke(Settings.currentSettings.rootMeanSquareStrokeWidth)
            color = Settings.currentSettings.rootMeanSquareColor
            for (i in 0 until data.size) {
                val rmsHeight = (data.rootMeanSquare[i] * height).toDouble() / data.maxPeak / 4
                val yAverage = (height - (data.averagePeaks[i] * height).toDouble() / data.maxPeak) / 2
                drawLine(i + data.skippedChunks, (yAverage - rmsHeight).toInt(), i + data.skippedChunks, (yAverage + rmsHeight).toInt())
            }
        }

        private fun Graphics2D.drawWords() {
            val usedRange = drawRange
            wordData.forEach {
                val coordinates = it.coordinates
                if (coordinates.intersects(usedRange)) {
                    drawCenteredWord(it.text, coordinates)
                }
                val (leftBound, rightBound) = coordinates
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

        private fun Graphics2D.drawCenteredWord(word: String, borders: ClosedIntRange) {
            val (x1, x2) = borders
            val stringWidth = getFontMetrics(wordFont).stringWidth(word)
            if (stringWidth < x2.toLong() - x1) {
                val pos = ((x2.toLong() + x1 - stringWidth) / 2).toInt()
                color = Settings.currentSettings.wordColor
                font = wordFont
                drawString(word, pos, height / 6)
            } else {
                color = Settings.currentSettings.wordSeparatorColor
                stroke = BasicStroke(Settings.currentSettings.wordSeparatorWidth,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,
                        0f,
                        FloatArray(1) { 10.0f },
                        0f)
                drawLine(x1, height / 6, x2, height / 6)
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

        private val Word.coordinates
            get() = ClosedIntRange(startMillisecond.toXCoordinate(), endMilliseconds.toXCoordinate())

        private fun Double.toXCoordinate() =
                statProvider.getChunkOfFrame(maximum, (this / statProvider.millisecondsPerFrame).toLong())
    }

    companion object {
        private const val maxSamplesPerChunk = 100000
        private const val minSamplesPerChunk = 20
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