package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.ui.components.JBScrollPane
import vladsaif.syncedit.plugin.MultimediaModel
import javax.swing.ScrollPaneConstants

class JScrollableWaveform(multimediaModel: MultimediaModel) : JBScrollPane(JWaveform(multimediaModel)) {
    val waveform = viewport.view as JWaveform
    val controller = WaveformController(waveform)
    val file get() = waveform.model.multimediaModel.audioFile

    init {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        waveform.installListeners()
        controller.installZoom(this)
    }
}