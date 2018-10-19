package vladsaif.syncedit.plugin.actions.tools

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import vladsaif.syncedit.plugin.actions.openScreencast
import vladsaif.syncedit.plugin.format.ScreencastFileType
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.util.ExEDT
import java.io.File
import java.nio.file.Path

open class OpenScreencast : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(ScreencastFileType)
    FileChooser.chooseFile(descriptor, project, null) {
      open(project, it)
    }
  }

  protected fun open(project: Project, file: VirtualFile) {
    launch {
      withContext(ExEDT) {
        val path = file.toPath()
        val screencast = ScreencastFile.get(path) ?: ScreencastFile.create(project, file.toPath())
        screencast.scriptPsi?.let {
          FileEditorManager.getInstance(project).openFile(it.virtualFile, true, true)
        }
        openScreencast(screencast)
      }
    }
  }
}

fun VirtualFile.toPath(): Path = File(this.path).toPath()