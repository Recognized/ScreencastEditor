package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.audioview.ColorSettings
import vladsaif.syncedit.plugin.audioview.waveform.impl.MultiSelectionModel
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.util.IRange
import vladsaif.syncedit.plugin.util.TextFormatter
import java.awt.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min

class JWaveform(multimediaModel: ScreencastFile) : JBPanel<JWaveform>(), ChangeListener {
  private val myWordFont
    get() = JBUI.Fonts.label()
  val model = WaveformModel(multimediaModel)
  var selectionModel = MultiSelectionModel()

  init {
    preferredSize = Dimension(Toolkit.getDefaultToolkit().screenSize.width, height)
  }

  /**
   * Activate mouse selection using drag and ctrl-shift combination,
   * enable popup balloon with word text when hovering over a word.
   */
  fun installListeners() {
    addMouseListener(selectionModel.dragListener)
    addMouseMotionListener(selectionModel.dragListener)
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
      background = UIUtil.getPanelBackground()
      clearRect(0, 0, width, height)
      drawWordsBackGround()
      drawSelectedRanges()
      drawHorizontalLine()
      drawAveragedWaveform(model.audioData[0])
      drawWords()
      drawMovingBorder()
      val position = model.playFramePosition.get()
      if (position != -1L) {
        drawPosition(position)
      }
    }
  }

  private fun Graphics2D.drawWordsBackGround() {
    val usedRange = model.drawRange
    val words = model.screencast.data?.words ?: return
    color = ColorSettings.MAPPING_HIGHLIGHT_COLOR
    for (word in words) {
      val (x1, x2) = model.getCoordinates(word)
      if (IRange(x1, x2).intersects(usedRange)) {
        fillRect(x1, 0, x2 - x1, height)
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

  private fun Graphics2D.drawSelectedRange(selected: IRange, border: IRange) {
    val selectedVisible = border.intersect(selected)
    color = ColorSettings.AUDIO_SELECTION_COLOR
    if (!selectedVisible.empty) {
      fillRect(selectedVisible.start, 0, selectedVisible.length, height)
    }
  }

  private fun Graphics2D.drawPosition(position: Long) {
    val x = model.getChunk(position)
    color = ColorSettings.AUDIO_PLAY_LINE_COLOR
    stroke = BasicStroke(ColorSettings.ROOT_MEAN_SQUARE_STROKE_WIDTH)
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
    val workRange = IRange.from(data.skippedChunks, data.size)
    val editedRanges = model.editionModel.editions.map { Pair(model.frameRangeToChunkRange(it.first), it.second) }
    val workPieces = mutableListOf<Pair<IRange, EditionModel.EditionType>>()
    editedRanges.forEach {
      val x = it.first intersect workRange
      if (!x.empty) {
        workPieces.add(Pair(x, it.second))
      }
    }
    stroke = BasicStroke(ColorSettings.PEAK_STROKE_WIDTH)
    drawWaveformPiece(workPieces, ColorSettings.AUDIO_PEAK_CUT_COLOR, ColorSettings.AUDIO_PEAK_COLOR) {
      val yTop = (height - (data.highestPeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
      val yBottom = (height - (data.lowestPeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
      drawLine(it, yTop.toInt(), it, yBottom.toInt())
    }
    stroke = BasicStroke(ColorSettings.ROOT_MEAN_SQUARE_STROKE_WIDTH)
    color = ColorSettings.AUDIO_RMS_COLOR
    drawWaveformPiece(workPieces, ColorSettings.AUDIO_RMS_CUT_COLOR, ColorSettings.AUDIO_RMS_COLOR) {
      val rmsHeight = (data.rootMeanSquare[it - data.skippedChunks] * height).toDouble() / data.maxPeak / 4
      val yAverage = (height - (data.averagePeaks[it - data.skippedChunks] * height).toDouble() / data.maxPeak) / 2
      drawLine(it, (yAverage - rmsHeight).toInt(), it, (yAverage + rmsHeight).toInt())
    }
  }

  private inline fun Graphics2D.drawWaveformPiece(workPieces: List<Pair<IRange, EditionModel.EditionType>>,
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
    val words = model.screencast.data?.words ?: return
    for (word in words) {
      val coordinates = model.getCoordinates(word)
      if (coordinates.intersects(usedRange)) {
        drawCenteredWord(word.filteredText, coordinates)
      }
      val (leftBound, rightBound) = coordinates
      color = ColorSettings.WORD_SEPARATOR_COLOR
      stroke = BasicStroke(ColorSettings.WORD_SEPARATOR_WIDTH,
          BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL,
          0f,
          FloatArray(1) { ColorSettings.DASH_WIDTH },
          0f)
      if (leftBound > usedRange.start) {
        drawLine(leftBound, 0, leftBound, height)
      }
      if (rightBound < usedRange.end) {
        drawLine(rightBound, 0, rightBound, height)
      }
    }
  }

  private fun Graphics2D.drawMovingBorder() {
    val x = selectionModel.movingBorder
    if (x == -1) return
    color = ColorSettings.WORD_MOVING_SEPARATOR_COLOR
    stroke = BasicStroke(ColorSettings.WORD_SEPARATOR_WIDTH,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_BEVEL,
        0f,
        FloatArray(1) { ColorSettings.DASH_WIDTH },
        0f)
    drawLine(x, 0, x, height)
  }

  private fun Graphics2D.drawCenteredWord(word: String, borders: IRange) {
    val (x1, x2) = borders
    val metrics = getFontMetrics(myWordFont)
    val stringWidth = metrics.stringWidth(word)
    color = ColorSettings.WORD_COLOR
    font = myWordFont
    if (stringWidth < x2.toLong() - x1) {
      val pos = ((x2.toLong() + x1 - stringWidth) / 2).toInt()
      drawString(word, pos, height / 6)
    } else {
      val pos = min(x1 + metrics.charWidth('m') / 2, x2)
      drawString(TextFormatter.createEllipsis(word, x2 - pos) { metrics.stringWidth(it) }, pos, height / 6)
    }
  }
}