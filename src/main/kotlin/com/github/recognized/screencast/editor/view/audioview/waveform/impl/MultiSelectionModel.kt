package com.github.recognized.screencast.editor.view.audioview.waveform.impl

import com.github.recognized.kotlin.ranges.extensions.inside
import com.github.recognized.kotlin.ranges.extensions.intersectWith
import com.github.recognized.screencast.editor.util.mulScale
import com.github.recognized.screencast.editor.view.audioview.waveform.ChangeNotifier
import com.github.recognized.screencast.editor.view.audioview.waveform.SelectionModel
import com.github.recognized.screencast.editor.view.audioview.waveform.WaveformModel
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.math.max
import kotlin.math.min

class MultiSelectionModel : SelectionModel, ChangeNotifier by DefaultChangeNotifier() {
  private lateinit var myLocator: WaveformModel
  private inline val myCoordinator get() = myLocator.screencast.coordinator
  private inline val myEditionsView get() = myLocator.audio.editionsModel
  private var myMoveRange: IntRange = IntRange.EMPTY
  private var myIsPressedOverBorder: Boolean = false
  private var myDraggedBorder: Border? = null
  private var myStartDifference: Int = -1
  private var myTempSelectedRange: IntRange = IntRange.EMPTY
    set(value) {
      field = value intersectWith myLocator.audioPixels
    }
  var movingBorder: Int = -1
    private set

  fun enableWordSelection(locator: WaveformModel) {
    this.myLocator = locator
  }

  override val selectedRange: IntRange get() = myTempSelectedRange

  override fun resetSelection() {
    myTempSelectedRange = IntRange.EMPTY
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
      val rangeUnderClick = myLocator.getContainingWordRange(e.x.mulScale() - myLocator.pixelOffset)
      if (rangeUnderClick.isEmpty()) return
      if (e.isControlKeyDown) {
        resetSelection()
        myTempSelectedRange = if (rangeUnderClick == selectedRange) {
          IntRange.EMPTY
        } else {
          rangeUnderClick
        }
      }
      fireStateChanged()
    }

    override fun mousePressed(e: MouseEvent?) {
      e ?: return
      super.mousePressed(e)
      resetSelection()
      fireStateChanged()
    }

    override fun onDragStarted(point: Point) {
      val start = dragStartEvent ?: return
      resetSelection()
      fireStateChanged()
      val border = borderUnderCursor(start)
      myIsPressedOverBorder = border != null && JBSwingUtilities.isLeftMouseButton(start)
      if (myIsPressedOverBorder && border != null /* for smart cast only */) {
        myStartDifference = point.x.mulScale() - myLocator.pixelOffset -
            if (border.isLeft) border.source.pixelRange.start
            else border.source.pixelRange.endInclusive
        myDraggedBorder = border
        myMoveRange = border.allowedPixelRange
      }
    }

    override fun onDrag(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isShiftDown && start.isLeftMouseButton -> dragControlSelection(start.point, point)
        myIsPressedOverBorder -> dragBorder(point)
        start.isLeftMouseButton && start.isControlKeyDown -> dragSelection(start.point, point)
      }
    }

    override fun onDragFinished(point: Point) {
      val start = dragStartEvent ?: return
      when {
        (start.isControlKeyDown || start.isShiftDown) && start.isLeftMouseButton -> dragSelectionFinished()
        myIsPressedOverBorder -> dragBorderFinished()
      }
    }
  }

  private fun dragControlSelection(start: Point, point: Point) {
    val x = start.x.mulScale() - myLocator.pixelOffset
    val pointX = point.x.mulScale() - myLocator.pixelOffset
    myTempSelectedRange = min(x, pointX)..max(x, pointX)
    fireStateChanged()
  }

  private fun dragSelection(start: Point, point: Point) {
    val x = start.x.mulScale() - myLocator.pixelOffset
    val pointX = point.x.mulScale() - myLocator.pixelOffset
    val border = min(x, pointX)..max(x, pointX)
    myTempSelectedRange = myLocator.getCoveredRange(border)
    fireStateChanged()
  }

  private fun dragSelectionFinished() {
    if (!myTempSelectedRange.isEmpty()) {
      fireStateChanged()
    }
  }

  private fun dragBorder(point: Point) {
    movingBorder = myMoveRange.inside(point.x.mulScale() - myLocator.pixelOffset - myStartDifference)
    fireStateChanged()
  }

  private fun dragBorderFinished() {
    val locator = myLocator
    val border = myDraggedBorder!!
    val moveMsBorder = border.allowedMsRange
    val coordinator = locator.screencast.coordinator
    val newMs =
      coordinator.toMilliseconds(myEditionsView.overlay(coordinator.toFrame(movingBorder)))
        .coerceIn(moveMsBorder)
    val newMsRange = if (border.isLeft) {
      newMs..border.source.word.range.endInclusive
    } else {
      border.source.word.range.start..newMs
    }
    movingBorder = -1
    myLocator.screencast.performModification {
      with(getEditable(myLocator.audio)) {
        changeRange(border.index, newMsRange)
      }
    }
  }

  private val MouseEvent.isControlKeyDown get() = UIUtil.isControlKeyDown(this)
  private val MouseEvent.isLeftMouseButton get() = JBSwingUtilities.isLeftMouseButton(this)

  private fun isOverBorder(e: MouseEvent): Boolean {
    val scaledPixel = e.x.mulScale() - myLocator.pixelOffset
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
    val scaledPixel = e.x.mulScale() - myLocator.pixelOffset
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
    val audioFrames = with(myLocator.audio.model) { 0..totalFrames }
    val audioPixels = myCoordinator.toPixelRange(myEditionsView.impose(audioFrames))
    val audioMs = myCoordinator.toMillisecondsRange(myEditionsView.impose(audioFrames))
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