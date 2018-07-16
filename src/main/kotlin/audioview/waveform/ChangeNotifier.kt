package vladsaif.syncedit.plugin.audioview.waveform

import javax.swing.event.ChangeListener

interface ChangeNotifier {

    fun fireStateChanged()

    fun addChangeListener(listener: ChangeListener)

    fun removeChangeListener(listener: ChangeListener)
}