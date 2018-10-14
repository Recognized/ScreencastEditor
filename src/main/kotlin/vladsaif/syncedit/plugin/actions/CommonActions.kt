package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.audioview.toolbar.ScreencastToolWindow
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException
import javax.swing.Icon

fun openTranscript(screencast: ScreencastFile) {
  val transcript = screencast.transcriptPsi
  if (transcript != null) {
    FileEditorManager.getInstance(screencast.project).openFile(transcript.virtualFile, true, true)
  } else {
    try {
      SpeechRecognizer.getCurrentRecognizer().checkRequirements()
      SpeechRecognizer.runRecognitionTask(screencast)
    } catch (ex: IOException) {
      errorRequirementsNotSatisfied(screencast.project, ex)
    }
  }
}

fun openScript(screencast: ScreencastFile) {
  val script = screencast.scriptFile
  if (script != null) {
    FileEditorManager.getInstance(screencast.project).openFile(script, true, true)
  }
}

fun openAudio(screencast: ScreencastFile) {
  try {
    ScreencastToolWindow.openScreencastFile(screencast)
  } catch (ex: UnsupportedAudioFileException) {
    errorUnsupportedAudioFile(screencast.project, screencast.file)
  } catch (ex: IOException) {
    errorIO(screencast.project, ex)
  }
}

fun saveChanges(screencast: ScreencastFile) {
  val savingFun = screencast.getLightSaveFunction()
}

fun DefaultActionGroup.addAction(
    what: String,
    desc: String?,
    icon: Icon?,
    action: () -> Unit,
    checkAvailable: () -> Boolean = { true }
) {
  this.add(object : AnAction(what, desc, icon) {
    override fun actionPerformed(event: AnActionEvent) {
      action()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = checkAvailable()
    }
  })
}