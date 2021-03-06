package com.github.recognized.screencast.editor.view.audioview.waveform

import javax.swing.event.ChangeListener

interface ChangeNotifier {
  var isNotificationSuppressed: Boolean

  fun fireStateChanged()

  fun addChangeListener(listener: ChangeListener)

  fun removeChangeListener(listener: ChangeListener)
}