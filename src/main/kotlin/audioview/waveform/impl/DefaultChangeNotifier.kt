package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class DefaultChangeNotifier : ChangeNotifier {
    private val listeners = mutableListOf<ChangeListener>()

    override fun fireStateChanged() {
        val event = ChangeEvent(this)
        listeners.forEach { it.stateChanged(event) }
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