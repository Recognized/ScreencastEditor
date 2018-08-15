package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.getElementBounds
import vladsaif.syncedit.plugin.lang.transcript.psi.wordsBetween

/**
 * Concatenates selected words into one big word.
 * Activated only if editor has selection.
 */
class ConcatenateAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    if (getStateContext(e.dataContext) != State.ENABLED) return
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val psi = PsiDocumentManager
        .getInstance(e.project!!)
        .getPsiFile(editor.document) as TranscriptPsiFile
    val selection = IRange(editor.selectionModel.selectionStart, editor.selectionModel.selectionEnd - 1)
    val model = psi.model ?: return
    val data = model.data ?: return
    concatenateWords(model, selection, psi)
    ApplicationManager.getApplication().runWriteAction {
      editor.document.setText(data.text)
    }
  }

  private fun concatenateWords(model: MultimediaModel, selection: IRange, psi: TranscriptPsiFile) {
    val bounds = getElementBounds(selection, psi) ?: return
    var first = -1
    var last = -1
    for (word in wordsBetween(bounds.first, bounds.second)) {
      if (first == -1) first = word.number
      last = word.number
    }
    if (first == -1) return
    model.concatenateWords(IRange(first, last))
  }

  override fun update(e: AnActionEvent?) {
    e ?: return
    val state = getStateContext(e.dataContext)
    e.presentation.isEnabled = state == State.ENABLED
    e.presentation.isVisible = state != State.HIDDEN
  }

  private fun getStateContext(context: DataContext): State {
    val project = context.getData(CommonDataKeys.PROJECT)
    val editor = context.getData(CommonDataKeys.EDITOR)
    if (project == null || editor == null) {
      return State.HIDDEN
    }
    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
    return when {
      psi is TranscriptPsiFile && editor.selectionModel.hasSelection() -> State.ENABLED
      psi is TranscriptPsiFile -> State.DISABLED
      else -> State.HIDDEN
    }
  }

  private enum class State {
    ENABLED, DISABLED, HIDDEN
  }

  companion object {
    private val LOG = logger<ConcatenateAction>()
  }
}
