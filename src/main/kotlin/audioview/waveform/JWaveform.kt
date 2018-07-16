package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.Settings
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.MultiSelectionModel
import java.awt.*
import java.nio.file.Path
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.math.max

private val logger = logger<JWaveform>()

class JWaveform(val file: Path) : JBPanel<JWaveform>(), ChangeListener {
    private val wordFont
        get() = JBUI.Fonts.label()
    val model = WaveformModel(file)
    var selectionModel = MultiSelectionModel()

    init {
        preferredSize = Dimension(Toolkit.getDefaultToolkit().screenSize.width, height)
    }

    /**
     * Activate mouse selection using drag and ctrl-shift combination,
     * enable popup balloon with word text when hovering over a word.
     */
    fun installListeners() {
        addMouseListener(selectionModel)
        addMouseMotionListener(selectionModel)
        addMouseMotionListener(WordHintBalloonListener(this, model))
        selectionModel.enableWordSelection(model)
        selectionModel.addChangeListener(this)
        model.addChangeListener(this)
    }

    override fun stateChanged(event: ChangeEvent?) {
        repaint()
        revalidate()
    }

    override fun paintComponent(graphics: Graphics?) {
        super.paintComponent(graphics)
        graphics ?: return
        with(graphics as Graphics2D) {
            clearRect(0, 0, width, height)
            drawSelectedRanges()
            drawHorizontalLine()
            drawAveragedWaveform(model.audioData[0])
            drawWords()
            if (model.playFramePosition != -1L) {
                drawPosition(model.playFramePosition)
            }
        }
    }

    /**
     * Draws current selected area.
     *
     * @see SelectionModel
     */
    private fun Graphics2D.drawSelectedRanges() {
        val usedRange = model.drawRange
        selectionModel.selectedRanges.forEach { drawSelectedRange(it, usedRange) }
    }

    private fun Graphics2D.drawSelectedRange(selected: ClosedIntRange, border: ClosedIntRange) {
        val selectedVisible = border.intersect(selected)
        color = Settings.currentSettings.selectionColor
        if (!selectedVisible.empty) {
            fillRect(selectedVisible.start, 0, selectedVisible.length, height)
        }
    }

    private fun Graphics2D.drawPosition(position: Long) {
        val x = model.getChunk(position)
        color = Settings.currentSettings.playLineColor
        stroke = BasicStroke(Settings.currentSettings.rootMeanSquareStrokeWidth)
        drawLine(x, 0, x, height)
    }

    /**
     * Draw horizontal in center of component showing the zero pitch of sound.
     */
    private fun Graphics2D.drawHorizontalLine() {
        color = Color.BLACK
        drawLine(model.drawRange.start, height / 2, model.drawRange.end, height / 2)
    }

    /**
     * Draw averaged sound data.
     *
     * All chunks are represented with vertical lines of one pixel width.
     * @see AveragedSampleData
     */
    private fun Graphics2D.drawAveragedWaveform(data: AveragedSampleData) {
        val settings = Settings.currentSettings
        val workRange = ClosedIntRange.from(data.skippedChunks, data.size)
        val editedRanges = model.editionModel.editions.map { Pair(model.frameRangeToChunkRange(it.first), it.second) }
        val workPieces = mutableListOf<Pair<ClosedIntRange, EditionModel.EditionType>>()
        editedRanges.forEach {
            val x = it.first intersect workRange
            if (!x.empty) {
                workPieces.add(Pair(x, it.second))
            }
        }
        stroke = BasicStroke(settings.peakStrokeWidth)
        drawWaveformPiece(workPieces, settings.peakCutColor, settings.peakColor) {
            val yTop = (height - (data.highestPeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
            val yBottom = (height - (data.lowestPeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
            drawLine(it, yTop.toInt(), it, yBottom.toInt())
        }
        stroke = BasicStroke(settings.rootMeanSquareStrokeWidth)
        color = settings.rootMeanSquareColor
        drawWaveformPiece(workPieces, settings.rootMeanSquareCutColor, settings.rootMeanSquareColor) {
            val rmsHeight = (data.rootMeanSquare[it - data.skippedChunks] * height).toDouble() / data.maxPeak / 4
            val yAverage = (height - (data.averagePeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
            drawLine(it, (yAverage - rmsHeight).toInt(), it, (yAverage + rmsHeight).toInt())
        }
    }

    private inline fun Graphics2D.drawWaveformPiece(workPieces: List<Pair<ClosedIntRange, EditionModel.EditionType>>,
                                                    cutColor: Color,
                                                    noChangeColor: Color,
                                                    painter: (Int) -> Unit) {
        var last = Int.MIN_VALUE
        loop@ for (piece in workPieces) {
            color = when (piece.second) {
                CUT -> cutColor
                NO_CHANGES -> noChangeColor
                MUTE -> continue@loop
            }
            val range = piece.first
            for (i in max(range.start, last)..range.end) {
                painter(i)
            }
            last = range.end
        }
    }

    /**
     * Draw words and their bounds.
     *
     * If word do not fit in its bounds, it is replaced with dashed line.
     */
    private fun Graphics2D.drawWords() {
        val usedRange = model.drawRange
        model.wordData.forEach {
            val coordinates = model.getCoordinates(it)
            if (coordinates.intersects(usedRange)) {
                drawCenteredWord(it.text, coordinates)
            }
            val (leftBound, rightBound) = coordinates
            color = Settings.currentSettings.wordSeparatorColor
            stroke = BasicStroke(Settings.currentSettings.wordSeparatorWidth,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,
                    0f,
                    FloatArray(1) { Settings.currentSettings.dashWidth },
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
                    FloatArray(1) { Settings.currentSettings.dashWidth },
                    0f)
            drawLine(x1, height / 6, x2, height / 6)
        }
    }
}