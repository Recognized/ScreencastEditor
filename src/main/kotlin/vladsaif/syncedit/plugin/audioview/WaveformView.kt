package vladsaif.syncedit.plugin.audioview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollBar
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.*
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.BoundedRangeModel
import javax.swing.DefaultBoundedRangeModel
import javax.swing.ScrollPaneConstants
import javax.swing.event.MouseInputAdapter
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
//        horizontalScrollBar.model = panel
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
        private var dataCache = listOf(AveragedSampleData())
        private val selectedRanges = ClosedIntRangeUnion()
        private var tempSelectedRange = ClosedIntRange.EMPTY_RANGE
        private var mousePressedStartCoordinate = 0
        private var previousBalloon: Balloon? = null
        var wordData = listOf<Word>()
            set(value) {
                field = value.sorted()
            }

        val mouseListener = object : MouseInputAdapter() {
            private var isControlDown = false

            override fun mouseMoved(e: MouseEvent?) {
                e ?: return
                if (UIUtil.isControlKeyDown(e)) {
                    val enclosingWord = getEnclosingWord(e.x)
                    showWordLabel(enclosingWord ?: return, e.point)
                } else {
                    ClosedIntRange.EMPTY_RANGE
                }
            }

            override fun mouseReleased(e: MouseEvent?) {
                e ?: return
                logger.info("mouse released")
                if (tempSelectedRange != ClosedIntRange.EMPTY_RANGE) {
                    selectedRanges.union(tempSelectedRange)
                    tempSelectedRange = ClosedIntRange.EMPTY_RANGE
                }
            }

            override fun mousePressed(e: MouseEvent?) {
                e ?: return
                logger.info("mouse pressed")
                mousePressedStartCoordinate = e.x
                if (!e.isShiftDown) {
                    selectedRanges.clear()
                    repaint()
                }
            }

            override fun mouseClicked(e: MouseEvent?) {
                e ?: return
                if (!JBSwingUtilities.isLeftMouseButton(e)) return
                val rangeUnderClick = getEnclosingWordRange(e.x)
                if (rangeUnderClick.empty) return
                logger.info("Clicked=$rangeUnderClick")
                if (e.isShiftDown) {
                    logger.info("Shift down")
                    if (rangeUnderClick in selectedRanges) {
                        selectedRanges.exclude(rangeUnderClick)
                    } else {
                        selectedRanges.union(rangeUnderClick)
                    }
                    logger.info(selectedRanges.toString())
                } else {
                    selectedRanges.clear()
                    selectedRanges.union(rangeUnderClick)
                }
                repaint()
            }

            override fun mouseDragged(e: MouseEvent?) {
                e ?: return
                if (isControlDown != UIUtil.isControlKeyDown(e)) mousePressedStartCoordinate = e.x
                isControlDown = UIUtil.isControlKeyDown(e)
                val border = ClosedIntRange(
                        min(mousePressedStartCoordinate, e.x),
                        max(mousePressedStartCoordinate, e.x)
                )
                tempSelectedRange = if (isControlDown) {
                    border
                } else {
                    getCoveredRange(border).also { println(it) }
                }
                repaint()
            }
        }

        init {
            maximum = Toolkit.getDefaultToolkit().screenSize.width - 1
            minimum = 0
            value = 0
            extent = maximum
            background = Color.WHITE
            addMouseListener(mouseListener)
            addMouseMotionListener(mouseListener)
            logger.info("Model constraints: min=$minimum, value=$value, extent=$extent, max=$maximum")
        }

        private fun showWordLabel(word: Word, point: Point) {
            if (previousBalloon?.wasFadedOut() == false) {
                previousBalloon?.hide()
            }
            previousBalloon = JBPopupFactory.getInstance().createBalloonBuilder(JBLabel(word.text))
                    .setFadeoutTime(3000)
                    .createBalloon()
                    .apply {
                        setAnimationEnabled(false)
                        show(RelativePoint(this@UnderlyingPanel, point), Balloon.Position.above)
                    }
        }

        private fun getCoveredRange(extent: ClosedIntRange): ClosedIntRange {
            val coordinates = wordData.map { it.coordinates }
            val left = coordinates.find { it.end >= extent.start }?.start ?: return ClosedIntRange.EMPTY_RANGE
            val right = coordinates.findLast { it.start <= extent.end }?.end ?: return ClosedIntRange.EMPTY_RANGE
            return ClosedIntRange(left, right)
        }

        override fun setMaximum(newMaximum: Int) {
            model.maximum = minOf(max(newMaximum, (statProvider.totalFrames / maxSamplesPerChunk).floorToInt()),
                    (statProvider.totalFrames / minSamplesPerChunk).floorToInt(),
                    Int.MAX_VALUE / minSamplesPerChunk)
        }

        private fun getEnclosingWord(coordinate: Int) = wordData.find { it.coordinates.contains(coordinate) }

        private fun getEnclosingWordRange(coordinate: Int): ClosedIntRange {
            val enclosingWord = wordData.find { it.coordinates.contains(coordinate) }
            return enclosingWord?.coordinates ?: ClosedIntRange.EMPTY_RANGE
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
            dataCache = listOf(AveragedSampleData())
            cachedRange = null
            selectedRanges.clear()
            tempSelectedRange = ClosedIntRange.EMPTY_RANGE
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
                if (cachedRange?.intersects(visibleRange) != true) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val result = statProvider.getAveragedSampleData(maximum, drawRange)
                        synchronized(this@UnderlyingPanel) {
                            dataCache = result
                            cachedRange = visibleRange
                        }
                        ApplicationManager.getApplication().invokeLater { this@UnderlyingPanel.repaint() }
                    }
                }
                graphics2d.drawAveragedWaveform(dataCache[0])
            }
            graphics2d.drawWords()
        }

        private fun Graphics2D.drawSelectedRanges() {
            val usedRange = drawRange
            drawSelectedRange(tempSelectedRange, usedRange)
            selectedRanges.ranges.forEach { drawSelectedRange(it, usedRange) }
        }

        private fun Graphics2D.drawSelectedRange(selected: ClosedIntRange, border: ClosedIntRange = drawRange) {
            val selectedVisible = border.intersect(selected)
            color = Settings.currentSettings.selectionColor
            if (!selectedVisible.empty) {
                fillRect(selectedVisible.start, 0, selectedVisible.length, height)
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