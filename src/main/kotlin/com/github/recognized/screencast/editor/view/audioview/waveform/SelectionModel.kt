package com.github.recognized.screencast.editor.view.audioview.waveform

interface SelectionModel : ChangeNotifier {
  val selectedRange: IntRange

  fun resetSelection()
}