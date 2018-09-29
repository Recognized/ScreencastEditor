package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.ui.DialogBuilder
import javax.swing.JSpinner
import javax.swing.SpinnerListModel

class ChooseRecognizerAction : AnAction(), PersistentStateComponent<ChooseRecognizerAction.State> {
  override fun actionPerformed(e: AnActionEvent) {
    val builder = DialogBuilder()
    with(builder) {
      val spinner = JSpinner()
      val elements = Extensions.getExtensions(SpeechRecognizer.EP_NAME).map { ElementWrapper(it) }
      println(elements)
      spinner.model = SpinnerListModel(elements)
      spinner.value = ElementWrapper(SpeechRecognizer.getCurrentRecognizer())
      setTitle("Choose recognizer")
      setCenterPanel(spinner)
      resizable(false)
      setOkOperation {
        SpeechRecognizer.setCurrentRecognizer((spinner.value as ElementWrapper).src)
        builder.dialogWrapper.close(0, true)
      }
      show()
    }
  }

  class State(val recognizerName: String = SpeechRecognizer.getCurrentRecognizer().name)

  override fun getState(): State? {
    return State()
  }

  override fun loadState(state: State) {
    for (recognizer in Extensions.getExtensions(SpeechRecognizer.EP_NAME)) {
      if (recognizer.name == state.recognizerName) {
        SpeechRecognizer.setCurrentRecognizer(recognizer)
      }
    }
  }

  private class ElementWrapper(val src: SpeechRecognizer) {
    override fun toString() = src.name

    override fun equals(other: Any?): Boolean {
      return other is ElementWrapper && other.src.name == src.name
    }

    override fun hashCode(): Int {
      return src.name.hashCode()
    }
  }
}