package com.github.recognized.screencast.editor.actions

import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.editor.view.toolbar.ScreencastToolWindow
import com.intellij.openapi.fileEditor.FileEditorManager
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

fun openScript(screencast: Screencast) {
  val script = screencast.scriptViewFile
  if (script != null) {
    FileEditorManager.getInstance(screencast.project).openFile(script, true, true)
  }
}

fun openScreencast(screencast: Screencast) {
  try {
    ScreencastToolWindow.openScreencastFile(screencast)
  } catch (ex: UnsupportedAudioFileException) {
    errorUnsupportedAudioFile(screencast.project, screencast.file)
  } catch (ex: IOException) {
    errorIO(screencast.project, ex.message)
  }
}
