package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.ClosedIntRange

interface SelectionModel : ChangeNotifier {
  val selectedRanges: List<ClosedIntRange>

  fun resetSelection()

  fun addSelection(range: ClosedIntRange)

  fun removeSelected(range: ClosedIntRange)
}