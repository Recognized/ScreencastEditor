package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.getSelectedWords
import vladsaif.syncedit.plugin.lang.transcript.psi.getWord

class TranscriptRenameHandler : RenameHandler {
  override fun isRenaming(dataContext: DataContext) = isAvailableOnDataContext(dataContext)

  override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
    val editor = getEditor(dataContext) ?: return false
    val file = CommonDataKeys.PSI_FILE.getData(dataContext) as? TranscriptPsiFile ?: return false
    return getWord(editor, file) != null || getSelectedWords(editor, file).any()
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
    editor ?: return
    if (file !is TranscriptPsiFile) return
    dataContext ?: return
    if (!isRenaming(dataContext)) return
    val word = getWord(editor, file)
    if (word != null) {
      InplaceRenamer.rename(editor, word)
    } else {
      val otherWord = getSelectedWords(editor, file).firstOrNull() ?: return
      InplaceRenamer.rename(editor, otherWord)
    }
  }

  override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
    throw UnsupportedOperationException()
  }

  companion object {

    private fun getEditor(context: DataContext?): Editor? {
      return CommonDataKeys.EDITOR.getData(context ?: return null)
    }
  }
}