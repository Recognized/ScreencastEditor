package com.github.recognized.screencast.editor.actions.project

import com.github.recognized.screencast.editor.actions.tools.OpenScreencast
import com.github.recognized.screencast.recorder.format.ScreencastFileType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class OpenScreencastFromProjectTree : OpenScreencast() {

  override fun actionPerformed(e: AnActionEvent) {
    val file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
    open(e.project!!, file)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.presentation.isEnabledAndVisible
        && e.project != null
        && e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == ScreencastFileType
  }
}
