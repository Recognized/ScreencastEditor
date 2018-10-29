package vladsaif.syncedit.plugin.editor.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import icons.ScreencastEditorIcons.*
import org.jetbrains.kotlin.idea.KotlinIcons
import vladsaif.syncedit.plugin.actions.addAction
import vladsaif.syncedit.plugin.actions.openScript
import vladsaif.syncedit.plugin.actions.openTranscript
import vladsaif.syncedit.plugin.actions.saveChanges
import vladsaif.syncedit.plugin.editor.EditorPane
import vladsaif.syncedit.plugin.editor.ZoomController
import vladsaif.syncedit.plugin.editor.audioview.waveform.WaveformController
import vladsaif.syncedit.plugin.model.ScreencastFile

object ScreencastToolWindow {
  private const val myToolWindowId = "Screencast Editor"

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
    toolWindow.icon = SCREENCAST_TOOL_WINDOW
    return toolWindow
  }

  fun openScreencastFile(screencast: ScreencastFile) {
    val editorPane = EditorPane(screencast)
    val audioPanel = ActionPanel(
      createAudioRelatedActionGroup(editorPane.waveformController!!, editorPane.zoomController),
      editorPane
    )
    audioPanel.disposeAction = { editorPane.waveformController.stopImmediately() }
    val controlPanel = ActionPanel(createMainActionGroup(screencast), audioPanel)
    Disposer.register(controlPanel, audioPanel)
    val content = ContentFactory.SERVICE.getInstance().createContent(controlPanel, screencast.name, false)
    val toolWindow = getToolWindow(screencast.project)
    toolWindow.contentManager.removeAllContents(true)
    toolWindow.contentManager.addContent(content)
    toolWindow.setAvailable(true, null)
    toolWindow.activate(null)
  }

  private fun createMainActionGroup(screencast: ScreencastFile): ActionGroup {
    with(DefaultActionGroup()) group@{
      addAction("Reproduce screencast", "Reproduce screencast", AllIcons.Ide.Macro.Recording_1, {
        ApplicationManager.getApplication().invokeLater {
          //          KotlinCompileUtil.compileAndRun(screencast.getPlayScript())
        }
      })
      addAction("Open transcript", "Open transcript in editor", TRANSCRIPT, {
        openTranscript(screencast)
      })
      addAction("Open GUI script", "Open GUI script in editor", KotlinIcons.SCRIPT, {
        openScript(screencast)
      })
      addAction("Save changes", "Save edited screencast", AllIcons.Actions.Menu_saveall, {
        saveChanges(screencast)
      })
      return this
    }
  }

  private fun createAudioRelatedActionGroup(
    controller: WaveformController,
    zoomController: ZoomController
  ): ActionGroup {
    with(DefaultActionGroup()) group@{
      with(controller) {
        addAction("Play", "Play audio", PLAY, this::play) {
          playState != PLAY
        }
        addAction("Pause", "Pause audio", PAUSE, this::pause) {
          playState == PLAY
        }
        addAction("Stop", "Stop audio", STOP, this::stopImmediately) {
          playState !is WaveformController.PlayState.Stopped
        }
        addAction("Undo", "Undo changes in selected area", AllIcons.Actions.Undo, this::undo, this::hasSelection)
        addAction("Clip", "Clip audio", DELETE, this::cutSelected, this::hasSelection)
        addAction("Mute", "Mute selected", VOLUME_OFF, this::muteSelected, this::hasSelection)
        addAction("Zoom in", "Zoom in", AllIcons.Graph.ZoomIn, zoomController::zoomIn)
        addAction("Zoom out", "Zoom out", AllIcons.Graph.ZoomOut, zoomController::zoomOut)
        return this@group
      }
    }
  }

//  private fun createScriptActionGroup(controller: ScriptController)
}