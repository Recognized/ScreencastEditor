package com.github.recognized.screencast.editor.actions.tools

import com.github.recognized.screencast.editor.actions.openScreencast
import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.editor.util.ExEDT
import com.github.recognized.screencast.recorder.format.ScreencastFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    GlobalScope.launch {
      withContext(ExEDT) {
        val path = file.toPath()
        val screencast = Screencast.get(path) ?: Screencast.create(project, file.toPath())
        screencast.scriptViewPsi?.let {
          FileEditorManager.getInstance(project).openFile(it.virtualFile, true, true)
        }
        openScreencast(screencast)
      }
    }
  }
}

fun VirtualFile.toPath(): Path = File(this.path).toPath()
