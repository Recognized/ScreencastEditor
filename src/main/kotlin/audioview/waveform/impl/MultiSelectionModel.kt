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
    private val _selectedRanges = ClosedIntRangeUnion()
    private var pressStartCoordinate = 0
    private var isControlDown = false
    private var locator: WaveformModel? = null
    private var cacheCoherent = false
    private var cacheSelectedRanges = listOf<ClosedIntRange>()
    private var tempSelectedRange = EMPTY_RANGE
        set(value) {
            if (tempSelectedRange != value) {
                cacheCoherent = false
                field = value
            }
        }

    fun enableWordSelection(locator: WaveformModel) {
        this.locator = locator
    }

    override val selectedRanges: List<ClosedIntRange>
        get() = when {
            cacheCoherent -> cacheSelectedRanges
            tempSelectedRange.empty -> _selectedRanges.ranges
            else -> _selectedRanges.ranges + tempSelectedRange
        }.also {
            cacheCoherent = true
            cacheSelectedRanges = it
        }

    override fun resetSelection() {
        _selectedRanges.clear()
        tempSelectedRange = EMPTY_RANGE
        cacheCoherent = false
    }

    override fun addSelection(range: ClosedIntRange) {
        _selectedRanges.union(range)
        cacheCoherent = false
    }

    override fun removeSelected(range: ClosedIntRange) {
        _selectedRanges.exclude(range)
        cacheCoherent = false
    }

    override fun mouseReleased(e: MouseEvent?) {
        e ?: return
        if (!tempSelectedRange.empty) {
            addSelection(tempSelectedRange)
            tempSelectedRange = EMPTY_RANGE
            fireStateChanged()
        }
    }

    override fun mousePressed(e: MouseEvent?) {
        e ?: return
        pressStartCoordinate = e.x
        if (!e.isShiftDown) {
            resetSelection()
            fireStateChanged()
        }
    }

    override fun mouseClicked(e: MouseEvent?) {
        e ?: return
        if (!JBSwingUtilities.isLeftMouseButton(e)) return
        val model = locator ?: return
        val rangeUnderClick = model.getContainingWordRange(e.x)
        println(rangeUnderClick)
        if (rangeUnderClick.empty) return
        if (e.isShiftDown) {
            if (rangeUnderClick in _selectedRanges) {
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
        if (isControlDown != UIUtil.isControlKeyDown(e)) pressStartCoordinate = e.x
        isControlDown = UIUtil.isControlKeyDown(e)
        val border = ClosedIntRange(min(pressStartCoordinate, e.x), max(pressStartCoordinate, e.x))
        tempSelectedRange = if (isControlDown) {
            border
        } else {
            locator?.getCoveredRange(border) ?: return
        }
        fireStateChanged()
    }
}