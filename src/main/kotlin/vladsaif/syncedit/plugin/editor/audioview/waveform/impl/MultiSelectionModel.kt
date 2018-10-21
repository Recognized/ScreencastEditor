package vladsaif.syncedit.plugin.editor.audioview.waveform.impl

import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.SelectionModel
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformModel
import vladsaif.syncedit.plugin.util.IntRangeUnion
import vladsaif.syncedit.plugin.util.empty
import vladsaif.syncedit.plugin.util.end
import vladsaif.syncedit.plugin.util.inside
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.math.max
import kotlin.math.min

class MultiSelectionModel : SelectionModel, ChangeNotifier by DefaultChangeNotifier() {
  private val mySelectedRanges = IntRangeUnion()
  private var myLocator: WaveformModel? = null
  private var myCacheCoherent = false
  private var myCacheSelectedRanges = listOf<IntRange>()
  private var myMoveRange: IntRange = IntRange.EMPTY
  private var myIsPressedOverBorder: Boolean = false
  private var myWordBorderIndex: Int = -1
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
      if (isOverBorder(e) && !e.isControlKeyDown) {
        e.component?.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
      } else {
        e.component?.cursor = Cursor.getDefaultCursor()
      }
    }

    override fun mouseClicked(e: MouseEvent?) {
      e ?: return
      super.mouseClicked(e)
      if (!e.isLeftMouseButton) return
      val model = myLocator ?: return
      val rangeUnderClick = model.getContainingWordRange(e.x)
      if (rangeUnderClick.empty) return
      if (e.isShiftDown) {
        if (rangeUnderClick in mySelectedRanges) {
          removeSelected(rangeUnderClick)
        } else {
          addSelection(rangeUnderClick)
        }
      } else {
        resetSelection()
        addSelection(rangeUnderClick)
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
      myIsPressedOverBorder = isOverBorder(start) && JBSwingUtilities.isRightMouseButton(start)
      if (myIsPressedOverBorder) {
        val number = getBorderNumber(start)
        myStartDifference = point.x - getBorder(number)
        myWordBorderIndex = number
        var leftBorder = 0
        var rightBorder = Int.MAX_VALUE
        if (number - 1 >= 0) {
          leftBorder = getBorder(number - 1)
        }
        if (number + 1 < myLocator!!.wordCoordinates.size * 2) {
          rightBorder = getBorder(number + 1)
        }
        myMoveRange = IntRange(leftBorder, rightBorder)
      }
    }

    override fun onDrag(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown && start.isLeftMouseButton -> dragControlSelection(start.point, point)
        myIsPressedOverBorder -> dragBorder(point)
        start.isLeftMouseButton -> dragSelection(start.point, point)
      }
    }

    override fun onDragFinished(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown && start.isLeftMouseButton -> dragSelectionFinished()
        myIsPressedOverBorder -> dragBorderFinished()
        start.isLeftMouseButton -> dragSelectionFinished()
      }
    }
  }

  private fun getBorder(index: Int): Int {
    return myLocator?.wordCoordinates?.get(index / 2)?.let {
      if (index % 2 == 0) {
        it.start
      } else {
        it.end
      }
    } ?: -1
  }

  private fun dragControlSelection(start: Point, point: Point) {
    myTempSelectedRange = IntRange(min(start.x, point.x), max(start.x, point.x))
    fireStateChanged()
  }

  private fun dragSelection(start: Point, point: Point) {
    val border = IntRange(min(start.x, point.x), max(start.x, point.x))
    myTempSelectedRange = myLocator?.getCoveredRange(border) ?: return
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
    movingBorder = myMoveRange.inside(point.x - myStartDifference)
    fireStateChanged()
  }

  private fun dragBorderFinished() {
    val locator = myLocator ?: return
    val model = myLocator?.screencast ?: return
    val audioModel = model.audioDataModel ?: return
    val data = model.data ?: return

    val oldWord = data[myWordBorderIndex / 2]
    var leftBorder = 0
    var rightBorder = Int.MAX_VALUE
    if (myWordBorderIndex - 1 >= 0) {
      leftBorder = data[(myWordBorderIndex - 1) / 2].range.let {
        if ((myWordBorderIndex - 1) % 2 == 0) {
          it.start
        } else {
          it.end
        }
      }
    }
    if (myWordBorderIndex + 1 < data.words.size * 2) {
      rightBorder = data[(myWordBorderIndex + 1) / 2].range.let {
        if ((myWordBorderIndex + 1) % 2 == 0) {
          it.start
        } else {
          it.end
        }
      }
    }
    val borderPx = IntRange(movingBorder, movingBorder)
    val frameRange = locator.coordinator.pixelRangeToFrameRange(borderPx)

    val moveMsBorder = IntRange(leftBorder, rightBorder)
    val newMs = moveMsBorder.inside(audioModel.frameRangeToMsRange(frameRange).start)
    val newMsRange = if (myWordBorderIndex % 2 == 0) {
      IntRange(newMs, oldWord.range.end)
    } else {
      IntRange(oldWord.range.start, newMs)
    }
    movingBorder = -1
    model.changeRange(myWordBorderIndex / 2, newMsRange)
  }

  private val MouseEvent.isControlKeyDown get() = UIUtil.isControlKeyDown(this)
  private val MouseEvent.isLeftMouseButton get() = JBSwingUtilities.isLeftMouseButton(this)

  private fun isOverBorder(e: MouseEvent): Boolean {
    return getBorderNumber(e) >= 0
  }

  // Return number of first border that includes this point
  // This needed for predictability of click behaviour
  private fun getBorderNumber(e: MouseEvent): Int {
    val borders = myLocator?.wordBorders ?: return -1
    for ((index, border) in borders.withIndex()) {
      if (e.x in border) return index
    }
    return -1
  }
}