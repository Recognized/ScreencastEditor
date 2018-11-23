package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.model.ScreencastFile

/**
 * Concatenates selected words into one big word.
 * Activated only if editor has selection.
 */
class ConcatenateAction : TranscriptRefactoringAction() {

  override fun doAction(model: ScreencastFile, words: List<TranscriptWord>) {
    val first = words.firstOrNull() ?: return
    val last = words.last()
    LOG.info("Concatenating: ${words.map { it.text }}")
    model.performModification {
      concatenateWords(IntRange(first.number, last.number))
    }
  }

  override fun update(e: AnActionEvent) {
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
