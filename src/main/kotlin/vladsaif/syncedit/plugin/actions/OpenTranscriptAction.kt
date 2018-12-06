package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import icons.ScreencastEditorIcons
import vladsaif.syncedit.plugin.actions.tools.SetCredentials
import vladsaif.syncedit.plugin.model.Screencast
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import vladsaif.syncedit.plugin.recognition.recognizers.GSpeechKit
import java.io.IOException

class OpenTranscriptAction(
  val screencast: Screencast,
  val getActiveAudio: () -> Screencast.Audio?
) : AnAction(
  "Open transcript", "Open transcript in editor",
  ScreencastEditorIcons.TRANSCRIPT
) {

  private var myIsInProgress = false

  override fun actionPerformed(e: AnActionEvent) {
    myIsInProgress = true
    val activeAudio = getActiveAudio()
    val transcript = activeAudio?.transcriptFile
    when {
      transcript != null -> FileEditorManager.getInstance(screencast.project).openFile(transcript, true, true)
      activeAudio != null -> try {
        SpeechRecognizer.getCurrentRecognizer().checkRequirements()
        SpeechRecognizer.runRecognitionTask(screencast, activeAudio) {
          runInEdt {
            myIsInProgress = false
          }
        }
      } catch (ex: IOException) {
        if (SpeechRecognizer.getCurrentRecognizer() is GSpeechKit) {
          suggestSetCredentials(e)
        } else {
          errorRequirementsNotSatisfied(screencast.project, ex)
        }
        myIsInProgress = false
      }
      else -> myIsInProgress = false
    }
  }

  private fun suggestSetCredentials(e: AnActionEvent) {
    val result = Messages.showYesNoDialog(
      screencast.project,
      "Credentials for cloud service account should be set before recognition is used. Would you like to set them?",
      "Credentials are not set",
      null
    )
    if (result == Messages.YES) {
      ActionUtil.invokeAction(SetCredentials(), e.dataContext, e.place, e.inputEvent, null)
    }
  }

  override fun update(e: AnActionEvent) {
    with(e.presentation) {
      isEnabled = !myIsInProgress && getActiveAudio() != null
      description = if (myIsInProgress) {
        "Recognition is in progress..."
      } else {
        "Open transcript in editor"
      }
    }
  }
}