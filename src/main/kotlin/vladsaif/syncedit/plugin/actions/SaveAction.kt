package vladsaif.syncedit.plugin.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import vladsaif.syncedit.plugin.model.Screencast

class SaveAction(val screencast: Screencast) :
  AnAction("Save changes", "Save edited screencast", AllIcons.Actions.Menu_saveall) {

  private var myIsInProgress = false

  override fun actionPerformed(e: AnActionEvent) {
    val savingFun = screencast.getLightSaveFunction()
    val saveTask = object : Task.Modal(screencast.project, "Saving ${screencast.name}...", false) {
      override fun run(indicator: ProgressIndicator) {
        try {
          savingFun(screencast.file)
          runInEdt {
            notifySuccessfullySaved(screencast)
          }
        } catch (ex: Throwable) {
          runInEdt {
            errorWhileSaving(screencast, ex)
          }
        } finally {
          runInEdt {
            myIsInProgress = false
          }
        }
      }
    }
    myIsInProgress = true
    ProgressManager.getInstance().run(saveTask)
  }

  override fun update(e: AnActionEvent) {
    with(e.presentation) {
      isEnabled = !myIsInProgress
      description = if (myIsInProgress) {
        "Action is in progress..."
      } else {
        "Save edited screencast"
      }
    }
  }
}