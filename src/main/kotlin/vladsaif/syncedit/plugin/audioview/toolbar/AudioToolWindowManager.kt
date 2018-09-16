package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import icons.ScreencastEditorIcons
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import vladsaif.syncedit.plugin.ExEDT
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel

object AudioToolWindowManager {
  private const val myToolWindowId = "Screencast Audio Editor"

  private fun getToolWindow(project: Project): ToolWindow {
    val manager = ToolWindowManager.getInstance(project)
    val toolWindow = manager.getToolWindow(myToolWindowId)
        ?: manager.registerToolWindow(myToolWindowId, true, ToolWindowAnchor.BOTTOM).also {
          it.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun contentAdded(event: ContentManagerEvent) = Unit

            override fun contentRemoveQuery(event: ContentManagerEvent) = Unit

            override fun selectionChanged(event: ContentManagerEvent) = Unit

            override fun contentRemoved(event: ContentManagerEvent) {
              it.setAvailable(false, null)
            }
          })
        }
    toolWindow.icon = ScreencastEditorIcons.EQUALIZER_TOOL_WINDOW
    return toolWindow
  }

  suspend fun openAudioFile(project: Project, virtualFile: VirtualFile): WaveformModel {
    val model = withContext(ExEDT) {
      MultimediaModel.getOrCreate(project, virtualFile)
    }
    withContext(CommonPool) {
      model.audioFile = virtualFile
    }
    return withContext(ExEDT) {
      val audioPanel = AudioToolWindowPanel(model)
      val content = ContentFactory.SERVICE.getInstance().createContent(
          audioPanel,
          virtualFile.nameWithoutExtension,
          false
      )
      val toolWindow = getToolWindow(project)
      toolWindow.contentManager.removeAllContents(true)
      toolWindow.contentManager.addContent(content)
      toolWindow.setAvailable(true, null)
      toolWindow.activate(null)
      audioPanel.wave.waveform.model
    }
  }
}