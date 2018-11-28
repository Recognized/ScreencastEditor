package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.fileEditor.FileEditorManager
import vladsaif.syncedit.plugin.editor.toolbar.ScreencastToolWindow
import vladsaif.syncedit.plugin.model.ScreencastFile
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

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
