package vladsaif.syncedit.plugin.audioview.waveform.impl

import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.IRange.Companion.EMPTY_RANGE
import vladsaif.syncedit.plugin.IRangeUnion
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.SelectionModel
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.math.max
import kotlin.math.min

class MultiSelectionModel : SelectionModel, ChangeNotifier by DefaultChangeNotifier() {
  private val mySelectedRanges = IRangeUnion()
  private var myLocator: WaveformModel? = null
  private var myCacheCoherent = false
  private var myCacheSelectedRanges = listOf<IRange>()
  private var myMoveRange: IRange = EMPTY_RANGE
  private var myIsPressedOverBorder: Boolean = false
  private var myWordBorderIndex: Int = -1
  private var myStartDifference: Int = -1
  private var myTempSelectedRange = EMPTY_RANGE
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

  override val selectedRanges: List<IRange>
    get() = when {
      myCacheCoherent -> myCacheSelectedRanges
      myTempSelectedRange.empty -> mySelectedRanges.ranges
      else -> mySelectedRanges.ranges + myTempSelectedRange
    }.also {
      myCacheCoherent = true
      myCacheSelectedRanges = it
    }

  override fun resetSelection() {
    mySelectedRanges.clear()
    myTempSelectedRange = EMPTY_RANGE
    myCacheCoherent = false
  }

  override fun addSelection(range: IRange) {
    mySelectedRanges.union(range)
    myCacheCoherent = false
  }

  override fun removeSelected(range: IRange) {
    mySelectedRanges.exclude(range)
    myCacheCoherent = false
  }

  val dragListener = object : MouseDragListener() {

    override fun mouseMoved(e: MouseEvent?) {
      e ?: return
      super.mouseMoved(e)
      if (isOverBorder(e)) {
        e.component?.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
      } else {
        e.component?.cursor = Cursor.getDefaultCursor()
      }
    }

    override fun mouseClicked(e: MouseEvent?) {
      e ?: return
      super.mouseClicked(e)
      if (!e.isLeftMousedButton) return
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
      myIsPressedOverBorder = isOverBorder(start)
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
        myMoveRange = IRange(leftBorder, rightBorder)
      }
    }

    override fun onDrag(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown -> dragControlSelection(start.point, point)
        myIsPressedOverBorder -> dragBorder(point)
        else -> dragSelection(start.point, point)
      }
    }

    override fun onDragFinished(point: Point) {
      val start = dragStartEvent ?: return
      when {
        start.isControlKeyDown -> dragSelectionFinished()
        myIsPressedOverBorder -> dragBorderFinished()
        else -> dragSelectionFinished()
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
    myTempSelectedRange = IRange(min(start.x, point.x), max(start.x, point.x))
    fireStateChanged()
  }

  private fun dragSelection(start: Point, point: Point) {
    val border = IRange(min(start.x, point.x), max(start.x, point.x))
    myTempSelectedRange = myLocator?.getCoveredRange(border) ?: return
    fireStateChanged()
  }

  private fun dragSelectionFinished() {
    if (!myTempSelectedRange.empty) {
      addSelection(myTempSelectedRange)
      myTempSelectedRange = EMPTY_RANGE
      fireStateChanged()
    }
  }

  private fun dragBorder(point: Point) {
    movingBorder = myMoveRange.inside(point.x - myStartDifference)
    fireStateChanged()
  }

  private fun dragBorderFinished() {
    val locator = myLocator ?: return
    val model = myLocator?.multimediaModel ?: return
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
    val borderPx = IRange(movingBorder, movingBorder)
    val frameRange = locator.chunkRangeToFrameRange(borderPx)

    val moveMsBorder = IRange(leftBorder, rightBorder)
    val newMs = moveMsBorder.inside(audioModel.frameRangeToMsRange(frameRange).start)
    val newMsRange = if (myWordBorderIndex % 2 == 0) {
      IRange(newMs, oldWord.range.end)
    } else {
      IRange(oldWord.range.start, newMs)
    }
    movingBorder = -1
    model.changeRange(myWordBorderIndex / 2, newMsRange)
  }

  private val MouseEvent.isControlKeyDown get() = UIUtil.isControlKeyDown(this)
  private val MouseEvent.isLeftMousedButton get() = JBSwingUtilities.isLeftMouseButton(this)

  private fun isOverBorder(e: MouseEvent): Boolean {
    return getBorderNumber(e) >= 0
  }

  private fun getBorderNumber(e: MouseEvent): Int {
    val rangeClick = IRange.from(e.x, 1)
    return myLocator?.wordBorders?.binarySearch(rangeClick, IRange.INTERSECTS_CMP) ?: -1
  }
}