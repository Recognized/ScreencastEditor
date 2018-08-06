package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import icons.ScreencastEditorIcons
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel

object AudioToolWindowManager {
  private const val myToolWindowId = "Screencast Audio Editor"

  private fun getToolWindow(project: Project): ToolWindow {
    val manager = ToolWindowManager.getInstance(project)
    val toolWindow = manager.getToolWindow(myToolWindowId)
        ?: manager.registerToolWindow(myToolWindowId, false, ToolWindowAnchor.BOTTOM)
    toolWindow.icon = ScreencastEditorIcons.MUSIC_1_TOOL_WINDOW
    return toolWindow
  }

  fun openAudioFile(project: Project, virtualFile: VirtualFile): WaveformModel {
    val toolWindow = getToolWindow(project)
    toolWindow.contentManager.removeAllContents(true)
    val model = MultimediaModel.getOrCreate(project, virtualFile)
    model.audioFile = virtualFile
    val audioPanel = AudioToolWindowPanel(model)
    val content = ContentFactory.SERVICE.getInstance().createContent(
        audioPanel,
        virtualFile.nameWithoutExtension,
        false
    )
    toolWindow.contentManager.addContent(content)
    return audioPanel.wave.waveform.model
  }
}