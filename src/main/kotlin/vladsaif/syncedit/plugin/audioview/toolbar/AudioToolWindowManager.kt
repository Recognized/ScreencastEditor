package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel
import java.nio.file.Path

object AudioToolWindowManager {
    private const val toolWindowId = "Screencast Audio Editor"

    fun getToolWindow(project: Project): ToolWindow {
        val manager = ToolWindowManager.getInstance(project)
        return manager.getToolWindow(toolWindowId)
                ?: manager.registerToolWindow(toolWindowId, false, ToolWindowAnchor.BOTTOM)
    }

    fun openAudioFile(project: Project, file: Path) : WaveformModel {
        val toolWindow = getToolWindow(project)
        toolWindow.contentManager.removeAllContents(true)
        val audioPanel = AudioToolWindowPanel(file)
        val content = ContentFactory.SERVICE.getInstance().createContent(audioPanel, "", false)
        content.disposer = audioPanel
        toolWindow.contentManager.addContent(content)
        return audioPanel.wave.waveform.model
    }
}