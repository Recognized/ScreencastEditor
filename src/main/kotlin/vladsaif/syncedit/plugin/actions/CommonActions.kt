package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.withContext
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.recognition.SpeechRecognizer
import vladsaif.syncedit.plugin.toolbar.ScreencastToolWindow
import vladsaif.syncedit.plugin.util.ExEDT
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

val lightSavingActor = actor<ScreencastFile>(CommonPool) {
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
    withContext(ExEDT) {

    }
  }
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