package vladsaif.syncedit.plugin.editor.toolbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.ui.SimpleToolWindowPanel
import javax.swing.JComponent

class ActionPanel(content: JComponent) : SimpleToolWindowPanel(false, false), Disposable {

  var disposeAction: () -> Unit = {}

  init {
    add(content)
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
    disposeAction()
  }
}