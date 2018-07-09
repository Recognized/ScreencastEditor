package vladsaif.syncedit.plugin.audioview

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.SimpleToolWindowPanel
import vladsaif.syncedit.plugin.Word
import java.nio.file.Path
import javax.swing.Icon

class AudioToolWindowPanel(file: Path) : SimpleToolWindowPanel(false), Disposable {
    private val waveformView: WaveformView = WaveformView(BasicSampleProvider(file))

    init {
        val group = DefaultActionGroup()
        group.addAction("Play", "Play audio", AllIcons.General.Run) { /* TODO */ }
        group.addAction("Pause", "Pause audio", AllIcons.Actions.Pause) { /* TODO */ }
        group.addAction("Clip", "Clip audio", AllIcons.Actions.Menu_cut) { waveformView.repaint(); }
        group.addAction("Zoom in", "Zoom in", AllIcons.Actions.Minimize) {
            waveformView.zoomIn()
            repaint()
        }
        group.addAction("Zoom out", "Zoom out", AllIcons.Actions.FindPlain) {
            waveformView.zoomOut()
            repaint()
        }
        setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false).component)
        add(waveformView)
        // TODO delete
        waveformView.wordData = listOf(
                Word("Some", 0.0, 1000.0),
                Word("sentence", 2000.0, 3000.0),
                Word("on", 3000.0, 4000.0),
                Word("the", 4000.0, 5000.0),
                Word("audio", 5000.0, 6000.0),
                Word("track", 6000.0, 7000.0)
        )
    }

    private fun DefaultActionGroup.addAction(what: String, desc: String?, icon: Icon, action: () -> Unit) {
        this.add(object : AnAction(what, desc, icon) {
            override fun actionPerformed(event: AnActionEvent?) {
                action()
            }
        })
    }

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}