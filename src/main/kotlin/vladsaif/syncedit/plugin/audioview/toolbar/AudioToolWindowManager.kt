package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import icons.ScreencastEditorIcons
import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.actions.errorIO
import vladsaif.syncedit.plugin.actions.errorUnsupportedAudioFile
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

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

  private fun openAudioFile(screencast: ScreencastFile) {
    val audioPanel = AudioToolWindowPanel(screencast)
    val content = ContentFactory.SERVICE.getInstance().createContent(audioPanel, screencast.audioName, false)
    val toolWindow = getToolWindow(screencast.project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(content)
    toolWindow.setAvailable(true, null)
    toolWindow.activate(null)
  }

  fun openAudio(screencast: ScreencastFile) {
    try {
      openAudioFile(screencast)
    } catch (ex: UnsupportedAudioFileException) {
      errorUnsupportedAudioFile(screencast.project, screencast.file)
    } catch (ex: IOException) {
      errorIO(screencast.project, ex)
    }
  }
}