package com.github.recognized.screencast.editor.view.audioview.waveform.impl

import com.github.recognized.screencast.editor.view.audioview.waveform.ChangeNotifier
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class DefaultChangeNotifier : ChangeNotifier {
  private val myListeners = mutableListOf<ChangeListener>()
  private var myNotificationsDisabled = false
  override var isNotificationSuppressed: Boolean
    get() = myNotificationsDisabled
    set(value) {
      myNotificationsDisabled = value
    }

  override fun fireStateChanged() {
    if (!myNotificationsDisabled) {
      val event = ChangeEvent(this)
      myListeners.forEach { it.stateChanged(event) }
    }
  }

  override fun removeChangeListener(listener: ChangeListener) {
    myListeners.remove(listener)
  }

  override fun addChangeListener(listener: ChangeListener) {
    if (listener !in myListeners) {
      myListeners.add(listener)
    }
  }
}