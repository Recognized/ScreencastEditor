package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class DefaultChangeNotifier : ChangeNotifier {
    private val listeners = mutableListOf<ChangeListener>()
    private var notificationsDisabled = false
    override var isNotificationSuppressed: Boolean
        get() = notificationsDisabled
        set(value) {
            notificationsDisabled = value
        }

    override fun fireStateChanged() {
        if (!notificationsDisabled) {
            val event = ChangeEvent(this)
            listeners.forEach { it.stateChanged(event) }
        }
    }

    override fun removeChangeListener(listener: ChangeListener) {
        listeners.remove(listener)
    }

    override fun addChangeListener(listener: ChangeListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }
}