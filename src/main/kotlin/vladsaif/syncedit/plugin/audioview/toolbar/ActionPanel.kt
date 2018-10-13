package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.ui.SimpleToolWindowPanel
import javax.swing.JComponent

class ActionPanel(actionGroup: ActionGroup, content: JComponent) : SimpleToolWindowPanel(false, false), Disposable {

  var disposeAction: () -> Unit = {}

  init {
    add(content)
    setToolbar(
        ActionManager.getInstance().createActionToolbar(
            ActionPlaces.TOOLBAR,
            actionGroup,
            false
        ).component
    )
  }

  override fun dispose() {
    disposeAction()
  }
}