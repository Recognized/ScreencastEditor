package vladsaif.syncedit.plugin.editor.audioview.waveform

interface SelectionModel : ChangeNotifier {
  val selectedRange: IntRange

  fun resetSelection()
}