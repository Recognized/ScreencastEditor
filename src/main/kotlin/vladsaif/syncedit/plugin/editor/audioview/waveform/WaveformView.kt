package vladsaif.syncedit.plugin.editor.audioview.waveform

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.audioview.WaveformGraphics
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DragXAxisListener
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.MultiSelectionModel
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.WordHintBalloonListener
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.util.*
import java.awt.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.math.min

class WaveformView(
  val model: WaveformModel
) :
  JBPanel<WaveformView>(),
  ChangeListener,
  DraggableXAxis,
  DrawingFixture by DrawingFixture.create(),
  Disposable {

  private val myCoordinator = model.screencast.coordinator
  private val myWordFont get() = JBUI.Fonts.label()
  private val myXAxisDrag = object : DragXAxisListener() {
    override fun onDragAction() {
      repaint()
    }

    override fun onDragFinishedAction(delta: Int) {
      model.fixWaveformDelta(delta)
      repaint()
    }
  }
  val selectionModel = MultiSelectionModel()

  /**
   * Activate mouse selection using drag and ctrl-shift combination,
   * enable popup balloon with word text when hovering over a word.
   */
  fun installListeners() {
    selectionModel.dragListener.install(this)
    addMouseMotionListener(WordHintBalloonListener(this, model))
    selectionModel.enableWordSelection(model)
    selectionModel.addChangeListener(this)
    model.audioDataModel.addChangeListener(this)
    model.addChangeListener(this)
  }

  override fun dispose() {
    model.audioDataModel.removeChangeListener(this)
    model.removeChangeListener(this)
  }

  override fun getPreferredSize(): Dimension {
    val superValue = super.getPreferredSize()
    val audio = model.audioDataModel
    return Dimension(
      myCoordinator.toPixel(audio.totalFrames + audio.offsetFrames).divScale() + 200,
      superValue.height
    )
  }

  override fun activateXAxisDrag() {
    myXAxisDrag.install(this)
    selectionModel.dragListener.uninstall(this)
  }

  override fun deactivateXAxisDrag() {
    myXAxisDrag.uninstall(this)
    selectionModel.dragListener.install(this)
  }

  override fun stateChanged(event: ChangeEvent?) {
    repaint()
    revalidate()
  }

  override fun paintComponent(graphics: Graphics?) {
    super.paintComponent(graphics)
    graphics ?: return
    with(graphics as Graphics2D) {
      setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      background = UIUtil.getPanelBackground()
      clearRect(0, 0, width, height)
      translate(myXAxisDrag.delta + model.pixelOffset.divScaleF().toDouble(), 0.0)
      drawWordsBackGround()
      drawSelectedRanges()
      drawEndLine()
      drawAveragedWaveform(model.audioData[0])
      drawWords()
      drawMovingBorder()
      val position = model.playFramePosition
      if (position != -1L) {
        drawPosition(position)
      }
    }
  }

  private fun Graphics2D.drawWordsBackGround() {
    val usedRange = model.drawRange.get()
    color = WaveformGraphics.MAPPING_HIGHLIGHT_COLOR
    for (word in model.wordsView) {
      val (x1, x2) = word.pixelRange
      if (IntRange(x1, x2).intersects(usedRange)) {
        fillRect(x1.divScaleF(), 0.0f, (x2 - x1).divScaleF(), height.toFloat())
      }
    }
  }

  private fun Graphics2D.drawEndLine() {
    color = WaveformGraphics.HORIZONTAL_LINE
    stroke = BasicStroke(1.0f)
    val audioRange = model.editionModel.impose(0 until model.audioDataModel.totalFrames)
    val startX = 0
    val halfHeight = (height / 2).toFloat()
    if (startX in model.drawRange.get()) {
      val pixStartX = startX.divScaleF()
      drawLine(pixStartX, 0.0f, pixStartX, height.toFloat())
    }
    val endX = model.screencast.coordinator.toPixel(audioRange.endInclusive)
    if (endX in model.drawRange.get()) {
      val pixEndX = endX.divScaleF()
      drawLine(pixEndX, 0.0f, pixEndX, height.toFloat())
    }
    val centerLineRange = startX..endX intersectWith model.drawRange.get()
    drawLine(centerLineRange.start.divScaleF(), halfHeight, centerLineRange.endInclusive.divScaleF(), halfHeight)
  }

  /**
   * Draws current selected area.
   *
   * @see SelectionModel
   */
  private fun Graphics2D.drawSelectedRanges() {
    val usedRange = model.drawRange.get()
    drawSelectedRange(selectionModel.selectedRange, usedRange)
  }

  private fun Graphics2D.drawSelectedRange(selected: IntRange, border: IntRange) {
    val selectedVisible = border.intersectWith(selected)
    color = WaveformGraphics.AUDIO_SELECTION_COLOR
    if (!selectedVisible.empty) {
      fillRect(selectedVisible.start.divScaleF(), 0.0f, selectedVisible.length.divScaleF(), height.toFloat())
    }
  }

  private fun Graphics2D.drawPosition(position: Long) {
    val x = model.screencast.coordinator.toPixel(position).divScaleF() - model.pixelOffset.divScaleF()
    color = WaveformGraphics.AUDIO_PLAY_LINE_COLOR
    stroke = BasicStroke(1.0f)
    drawLine(x, 0.0f, x, height.toFloat())
  }

  /**
   * Draw averaged sound data.
   *
   * All chunks are represented with vertical lines of one pixel width.
   * @see AveragedSampleData
   */
  private fun Graphics2D.drawAveragedWaveform(data: AveragedSampleData) {
    val offsetChunk = 0
    var skipCut = 0
    val workRange = offsetChunk + data.skippedChunks until offsetChunk + data.skippedChunks + data.size
    stroke = BasicStroke(1.0f)
    for ((range, type) in model.editionModel.editions) {
      val screenRange = myCoordinator.toPixelRange(range)
      when (type) {
        NO_CHANGES -> {
          for (x in screenRange intersectWith workRange) {
            val index = x - offsetChunk - data.skippedChunks
            val scaledX = (x - skipCut).divScaleF()
            val yTop = (height - (data.highestPeaks[index] * height).toDouble() / data.maxPeak) / 2
            val yBottom = (height - (data.lowestPeaks[index] * height).toDouble() / data.maxPeak) / 2
            color = WaveformGraphics.AUDIO_PEAK_COLOR
            drawLine(scaledX, yTop.toFloat(), scaledX, yBottom.toFloat())
            val rmsHeight = (data.rootMeanSquare[index] * height).toDouble() / data.maxPeak / 4
            val yAverage = (height - (data.averagePeaks[index] * height).toDouble() / data.maxPeak) / 2
            color = WaveformGraphics.AUDIO_RMS_COLOR
            drawLine(scaledX, (yAverage - rmsHeight).toFloat(), scaledX, (yAverage + rmsHeight).toFloat())
          }
        }
        MUTE -> {
          color = WaveformGraphics.AUDIO_PEAK_COLOR
          for (x in screenRange intersectWith workRange) {
            val scaledX = (x - skipCut).divScaleF()
            drawLine(scaledX, (height / 2).toFloat(), scaledX, (height / 2).toFloat())
          }
        }
        CUT -> skipCut += screenRange.length
      }
    }
  }

  private fun Graphics2D.drawWords() {
    val usedRange = model.drawRange.get()
    for (word in model.wordsView) {
      val coordinates = word.pixelRange
      if (coordinates.intersects(usedRange)) {
        drawCenteredWord(word.word.text, coordinates)
      }
      val leftBound = word.pixelRange.start.divScaleF()
      val rightBound = word.pixelRange.endInclusive.divScaleF()
      color = WaveformGraphics.WORD_SEPARATOR_COLOR
      stroke = BasicStroke(
        WaveformGraphics.WORD_SEPARATOR_WIDTH,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_BEVEL,
        0f,
        FloatArray(1) { WaveformGraphics.DASH_WIDTH },
        0f
      )
      if (word.pixelRange.start > usedRange.start) {
        drawLine(leftBound, 0.0f, leftBound, height.toFloat())
      }
      if (word.pixelRange.endInclusive < usedRange.endInclusive) {
        drawLine(rightBound, 0.0f, rightBound, height.toFloat())
      }
    }
  }

  private fun Graphics2D.drawMovingBorder() {
    if (selectionModel.movingBorder == -1) return
    val x = selectionModel.movingBorder.divScaleF()
    color = WaveformGraphics.WORD_MOVING_SEPARATOR_COLOR
    stroke = BasicStroke(
      1.0f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_BEVEL,
      0f,
      FloatArray(1) { WaveformGraphics.DASH_WIDTH },
      0f
    )
    drawLine(x, 0.0f, x, height.toFloat())
  }

  private fun Graphics2D.drawCenteredWord(word: String, borders: IntRange) {
    val (x1, x2) = borders.mapInt { it.divScale() }
    val metrics = getFontMetrics(myWordFont)
    val stringWidth = metrics.stringWidth(word)
    color = WaveformGraphics.WORD_COLOR
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