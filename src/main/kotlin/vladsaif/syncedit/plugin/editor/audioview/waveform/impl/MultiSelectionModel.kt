package vladsaif.syncedit.plugin.editor.audioview.waveform.impl

import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.SelectionModel
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformModel
import vladsaif.syncedit.plugin.util.IntRangeUnion
import vladsaif.syncedit.plugin.util.empty
import vladsaif.syncedit.plugin.util.inside
import vladsaif.syncedit.plugin.util.mulScale
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.math.max
import kotlin.math.min

class MultiSelectionModel : SelectionModel, ChangeNotifier by DefaultChangeNotifier() {
  private val mySelectedRanges = IntRangeUnion()
  private lateinit var myLocator: WaveformModel
  private var myCacheCoherent = false
  private var myCacheSelectedRanges = listOf<IntRange>()
  private var myMoveRange: IntRange = IntRange.EMPTY
  private var myIsPressedOverBorder: Boolean = false
  private var myDraggedBorder: Border? = null
  private var myStartDifference: Int = -1
  private var myTempSelectedRange: IntRange = IntRange.EMPTY
    set(value) {
      if (myTempSelectedRange != value) {
        myCacheCoherent = false
        field = value
      }
    }
  var movingBorder: Int = -1
    private set

  fun enableWordSelection(locator: WaveformModel) {
    this.myLocator = locator
  }

  override val selectedRanges: List<IntRange>
    get() = when {
      myCacheCoherent -> myCacheSelectedRanges
      myTempSelectedRange.empty -> mySelectedRanges.ranges
      else -> mySelectedRanges.ranges.toMutableList().also { it.add(myTempSelectedRange) }
    }.also { it: List<IntRange> ->
      myCacheCoherent = true
      myCacheSelectedRanges = it
    }

  override fun resetSelection() {
    mySelectedRanges.clear()
    myTempSelectedRange = IntRange.EMPTY
    myCacheCoherent = false
  }

  override fun addSelection(range: IntRange) {
    mySelectedRanges.union(range)
    myCacheCoherent = false
  }

  override fun removeSelected(range: IntRange) {
    mySelectedRanges.exclude(range)
    myCacheCoherent = false
  }

  val dragListener = object : MouseDragListener() {

    override fun mouseMoved(e: MouseEvent?) {
      e ?: return
      super.mouseMoved(e)
      if (!e.isShiftDown && !e.isControlKeyDown && isOverBorder(e)) {
        e.component?.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
      } else {
        e.component?.cursor = Cursor.getDefaultCursor()
      }
    }

    override fun mouseClicked(e: MouseEvent?) {
      e ?: return
      super.mouseClicked(e)
      if (!e.isLeftMouseButton) return
      val rangeUnderClick = myLocator.getContainingWordRange(e.x.mulScale())
      if (rangeUnderClick.empty) return
      if (e.isShiftDown) {
        resetSelection()
        if (rangeUnderClick in mySelectedRanges) {
          removeSelected(rangeUnderClick)
        } else {
          addSelection(rangeUnderClick)
        }
      }
      fireStateChanged()
    }

    override fun mousePressed(e: MouseEvent?) {
      e ?: return
      super.mousePressed(e)
      if (!e.isShiftDown) {
        resetSelection()
        fireStateChanged()
      }
    }

    override fun onDragStarted(point: Point) {
      val start = dragStartEvent ?: return
      if (!start.isShiftDown) {
        resetSelection()
        fireStateChanged()
      }
      val border = borderUnderCursor(start)
      myIsPressedOverBorder = border != null && JBSwingUtilities.isLeftMouseButton(start)
      if (myIsPressedOverBorder && border != null /* for smart cast only */) {
        myStartDifference = point.x.mulScale() -
            if (border.isLeft) border.source.pixelRange.start
            else border.source.pixelRange.endInclusive
        myDraggedBorder = border
        myMoveRange = border.allowedPixelRange
      }
    }

    override fun onDrag(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown && start.isLeftMouseButton -> dragControlSelection(start.point, point)
        myIsPressedOverBorder -> dragBorder(point)
        start.isLeftMouseButton && start.isShiftDown -> dragSelection(start.point, point)
      }
    }

    override fun onDragFinished(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown && start.isLeftMouseButton -> dragSelectionFinished()
        myIsPressedOverBorder -> dragBorderFinished()
        start.isLeftMouseButton && start.isShiftDown -> dragSelectionFinished()
      }
    }
  }

  private fun dragControlSelection(start: Point, point: Point) {
    val x = start.x.mulScale()
    val pointX = point.x.mulScale()
    myTempSelectedRange = IntRange(min(x, pointX), max(x, pointX))
    fireStateChanged()
  }

  private fun dragSelection(start: Point, point: Point) {
    val x = start.x.mulScale()
    val pointX = point.x.mulScale()
    val border = IntRange(min(x, pointX), max(x, pointX))
    myTempSelectedRange = myLocator.getCoveredRange(border)
    fireStateChanged()
  }

  private fun dragSelectionFinished() {
    if (!myTempSelectedRange.empty) {
      addSelection(myTempSelectedRange)
      myTempSelectedRange = IntRange.EMPTY
      fireStateChanged()
    }
  }

  private fun dragBorder(point: Point) {
    movingBorder = myMoveRange.inside(point.x.mulScale() - myStartDifference)
    fireStateChanged()
  }

  private fun dragBorderFinished() {
    val locator = myLocator
    val model = myLocator.screencast
    val border = myDraggedBorder!!
    val moveMsBorder = border.allowedMsRange
    val editionModel = model.editionModel
    val coordinator = locator.screencast.coordinator
    val newMs =
      coordinator.toMilliseconds(editionModel.overlay(coordinator.toFrame(movingBorder)))
        .coerceIn(moveMsBorder)
    val newMsRange = if (border.isLeft) {
      newMs..border.source.word.range.endInclusive
    } else {
      border.source.word.range.start..newMs
    }
    movingBorder = -1
    model.changeRange(border.index, newMsRange)
  }

  private val MouseEvent.isControlKeyDown get() = UIUtil.isControlKeyDown(this)
  private val MouseEvent.isLeftMouseButton get() = JBSwingUtilities.isLeftMouseButton(this)

  private fun isOverBorder(e: MouseEvent): Boolean {
    val scaledPixel = e.x.mulScale()
    for (wordView in myLocator.wordsView) {
      if (scaledPixel in wordView.leftBorder || scaledPixel in wordView.rightBorder) {
        return true
      }
    }
    return false
  }

  private fun borderUnderCursor(e: MouseEvent): Border? {
    var index = -1
    var isLeft = false
    val scaledPixel = e.x.mulScale()
    for ((i, wordView) in myLocator.wordsView.withIndex()) {
      if (scaledPixel in wordView.leftBorder) {
        isLeft = true
        index = i
        break
      }
      if (scaledPixel in wordView.rightBorder) {
        isLeft = false
        index = i
        break
      }
    }
    if (index == -1) return null
    val coordinator = myLocator.screencast.coordinator
    val editionModel = myLocator.screencast.editionModel
    val audioFrames = with(myLocator.audioDataModel) { offsetFrames..offsetFrames + totalFrames }
    val audioPixels = coordinator.toPixelRange(editionModel.impose(audioFrames))
    val audioMs = coordinator.toMillisecondsRange(editionModel.impose(audioFrames))
    var leftBorder = audioPixels.start
    var rightBorder = audioPixels.endInclusive
    var msLeft = audioMs.start
    var msRight = audioMs.endInclusive
    val source: WaveformModel.WordView = myLocator.wordsView[index]
    if (isLeft) {
      if (index - 1 >= 0) {
        leftBorder = myLocator.wordsView[index - 1].pixelRange.endInclusive.coerceIn(audioPixels)
        msLeft = myLocator.wordsView[index - 1].word.range.endInclusive
      }
      rightBorder = source.pixelRange.endInclusive.coerceIn(audioPixels)
      msRight = source.word.range.endInclusive
    } else {
      leftBorder = source.pixelRange.start.coerceIn(audioPixels)
      msLeft = source.word.range.start
      if (index + 1 < myLocator.wordsView.size) {
        rightBorder = myLocator.wordsView[index + 1].pixelRange.start.coerceIn(audioPixels)
        msRight = myLocator.wordsView[index + 1].word.range.start
      }
    }
    return Border(source, index, isLeft, leftBorder..rightBorder, msLeft..msRight)
  }

  data class Border(
    val source: WaveformModel.WordView,
    val index: Int,
    val isLeft: Boolean,
    val allowedPixelRange: IntRange,
    val allowedMsRange: IntRange
  )
}