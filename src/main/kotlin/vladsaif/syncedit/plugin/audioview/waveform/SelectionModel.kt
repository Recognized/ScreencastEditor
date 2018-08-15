package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.IRange

interface SelectionModel : ChangeNotifier {
  val selectedRanges: List<IRange>

  fun resetSelection()

  fun addSelection(range: IRange)

  fun removeSelected(range: IRange)
}