package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.SimpleToolWindowPanel
import vladsaif.syncedit.plugin.audioview.waveform.JScrollableWaveform
import java.nio.file.Path
import javax.swing.Icon

class AudioToolWindowPanel(file: Path) : SimpleToolWindowPanel(false), Disposable {
    val wave = JScrollableWaveform(file)

    init {
        add(wave)
        val group = DefaultActionGroup()
        group.addAction("Play", "Play audio", AllIcons.General.Run, wave.controller::play)
        group.addAction("Pause", "Pause audio", AllIcons.Actions.Pause, wave.controller::pause)
        group.addAction("Stop", "Stop audio", AllIcons.Actions.Cancel, wave.controller::stop)
        group.addAction("Undo", "Undo changed in selected area", AllIcons.Actions.Undo, wave.controller::undo)
        group.addAction("Clip", "Clip audio", AllIcons.Actions.Menu_cut, wave.controller::cutSelected)
        group.addAction("Mute", "Mute selected", AllIcons.Actions.Exclude, wave.controller::muteSelected)
        group.addAction("Zoom in", "Zoom in", AllIcons.Graph.ZoomIn, wave.controller::zoomIn)
        group.addAction("Zoom out", "Zoom out", AllIcons.Graph.ZoomOut, wave.controller::zoomOut)
        setToolbar(ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, false).component)
    }

    private fun DefaultActionGroup.addAction(what: String, desc: String?, icon: Icon, action: () -> Unit) {
        this.add(object : AnAction(what, desc, icon) {
            override fun actionPerformed(event: AnActionEvent?) {
                action()
            }
        })
    }

    override fun dispose() {
        wave.controller.dispose()
    }
}