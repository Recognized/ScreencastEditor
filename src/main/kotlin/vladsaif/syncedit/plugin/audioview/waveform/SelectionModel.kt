package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.ClosedIntRange
import javax.swing.event.MouseInputListener

interface SelectionModel : ChangeNotifier, MouseInputListener {
    val selectedRanges: List<ClosedIntRange>

    fun resetSelection()

    fun addSelection(range: ClosedIntRange)

    fun removeSelected(range: ClosedIntRange)
}