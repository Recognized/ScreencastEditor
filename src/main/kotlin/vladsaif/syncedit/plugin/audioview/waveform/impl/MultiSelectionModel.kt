package vladsaif.syncedit.plugin.audioview.waveform.impl

import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.ClosedIntRange.Companion.EMPTY_RANGE
import vladsaif.syncedit.plugin.ClosedIntRangeUnion
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.SelectionModel
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel
import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter
import kotlin.math.max
import kotlin.math.min

class MultiSelectionModel : MouseInputAdapter(), SelectionModel, ChangeNotifier by DefaultChangeNotifier() {
  private val mySelectedRanges = ClosedIntRangeUnion()
  private var myPressStartCoordinate = 0
  private var myIsControlDown = false
  private var myLocator: WaveformModel? = null
  private var myCacheCoherent = false
  private var myCacheSelectedRanges = listOf<ClosedIntRange>()
  private var myTempSelectedRange = EMPTY_RANGE
    set(value) {
      if (myTempSelectedRange != value) {
        myCacheCoherent = false
        field = value
      }
    }

  fun enableWordSelection(locator: WaveformModel) {
    this.myLocator = locator
  }

  override val selectedRanges: List<ClosedIntRange>
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

  override fun addSelection(range: ClosedIntRange) {
    mySelectedRanges.union(range)
    myCacheCoherent = false
  }

  override fun removeSelected(range: ClosedIntRange) {
    mySelectedRanges.exclude(range)
    myCacheCoherent = false
  }

  override fun mouseReleased(e: MouseEvent?) {
    e ?: return
    if (!myTempSelectedRange.empty) {
      addSelection(myTempSelectedRange)
      myTempSelectedRange = EMPTY_RANGE
      fireStateChanged()
    }
  }

  override fun mousePressed(e: MouseEvent?) {
    e ?: return
    myPressStartCoordinate = e.x
    if (!e.isShiftDown) {
      resetSelection()
      fireStateChanged()
    }
  }

  override fun mouseClicked(e: MouseEvent?) {
    e ?: return
    if (!JBSwingUtilities.isLeftMouseButton(e)) return
    val model = myLocator ?: return
    val rangeUnderClick = model.getContainingWordRange(e.x)
    println(rangeUnderClick)
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

  override fun mouseDragged(e: MouseEvent?) {
    e ?: return
    if (myIsControlDown != UIUtil.isControlKeyDown(e)) myPressStartCoordinate = e.x
    myIsControlDown = UIUtil.isControlKeyDown(e)
    val border = ClosedIntRange(min(myPressStartCoordinate, e.x), max(myPressStartCoordinate, e.x))
    myTempSelectedRange = if (myIsControlDown) {
      border
    } else {
      myLocator?.getCoveredRange(border) ?: return
    }
    fireStateChanged()
  }
}