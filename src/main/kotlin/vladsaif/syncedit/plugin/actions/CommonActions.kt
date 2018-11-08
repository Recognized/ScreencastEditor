package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import vladsaif.syncedit.plugin.editor.toolbar.ScreencastToolWindow
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

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
  val script = screencast.scriptViewFile
  if (script != null) {
    FileEditorManager.getInstance(screencast.project).openFile(script, true, true)
  }
}

fun openScreencast(screencast: ScreencastFile) {
  try {
    ScreencastToolWindow.openScreencastFile(screencast)
  } catch (ex: UnsupportedAudioFileException) {
    errorUnsupportedAudioFile(screencast.project, screencast.file)
  } catch (ex: IOException) {
    errorIO(screencast.project, ex.message)
  }
}

fun saveChanges(screencast: ScreencastFile) {
  lightSavingActor.offer(screencast)
}

val lightSavingActor = GlobalScope.actor<ScreencastFile>(Dispatchers.Default) {
  for (screencast in channel) {
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
        }
      }
    }
    ProgressManager.getInstance().run(saveTask)
  }
}