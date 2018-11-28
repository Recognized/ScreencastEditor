package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import icons.ScreencastEditorIcons
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException

class OpenTranscriptAction(val screencast: ScreencastFile) : AnAction(
  "Open transcript", "Open transcript in editor",
  ScreencastEditorIcons.TRANSCRIPT
) {

  private var myIsInProgress = false

  override fun actionPerformed(e: AnActionEvent) {
    myIsInProgress = true
    val transcript = screencast.transcriptPsi
    if (transcript != null) {
      FileEditorManager.getInstance(screencast.project).openFile(transcript.virtualFile, true, true)
    } else {
      try {
        SpeechRecognizer.getCurrentRecognizer().checkRequirements()
        SpeechRecognizer.runRecognitionTask(screencast) {
          runInEdt {
            myIsInProgress = false
          }
        }
      } catch (ex: IOException) {
        errorRequirementsNotSatisfied(screencast.project, ex)
        myIsInProgress = false
      }
    }
  }

  override fun update(e: AnActionEvent) {
    with(e.presentation) {
      isEnabled = !myIsInProgress
      description = if (myIsInProgress) {
        "Recognition is in progress..."
      } else {
        "Open transcript in editor"
      }
    }
  }
}