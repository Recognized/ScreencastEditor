package com.github.recognized.screencast.editor.view.toolbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

class ActionPanel(content: JComponent) : SimpleToolWindowPanel(false, false), Disposable {

  init {
    add(content)
    if (content is Disposable) {
      Disposer.register(this, content)
    }
  }

  fun addActionGroups(group: ActionGroup) {
    setToolbar(
      ActionManager.getInstance().createActionToolbar(
        ActionPlaces.TOOLBAR,
        group,
        false
      ).component
    )
    isRequestFocusEnabled = true
  }

  override fun dispose() {
  }
}