package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import icons.ScreencastEditorIcons
import vladsaif.syncedit.plugin.model.Screencast
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
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
        errorRequirementsNotSatisfied(screencast.project, ex)
        myIsInProgress = false
      }
      else -> myIsInProgress = false
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