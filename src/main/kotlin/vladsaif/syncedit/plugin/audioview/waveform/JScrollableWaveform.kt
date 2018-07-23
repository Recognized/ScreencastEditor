package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.ui.components.JBScrollPane
import java.nio.file.Path
import javax.swing.ScrollPaneConstants

class JScrollableWaveform(val file: Path) : JBScrollPane(JWaveform(file)) {
    val waveform = viewport.view as JWaveform
    val controller = WaveformController(waveform)

    init {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        waveform.installListeners()
        controller.installZoom(this)
    }
}