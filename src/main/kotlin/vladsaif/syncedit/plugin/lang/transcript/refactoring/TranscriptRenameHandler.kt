package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.lang.transcript.psi.getElementBounds
import vladsaif.syncedit.plugin.lang.transcript.psi.wordsBetween
import kotlin.math.max
import kotlin.math.min

private val logger = logger<TranscriptRenameHandler>()

class TranscriptRenameHandler : RenameHandler {
    override fun isRenaming(dataContext: DataContext?) = isAvailableOnDataContext(dataContext)

    override fun isAvailableOnDataContext(dataContext: DataContext?): Boolean {
        dataContext ?: return false
        val editor = getEditor(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) as? TranscriptPsiFile ?: return false
        return getWord(editor, file) != null || getSelectedWords(editor, file).any()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        editor ?: return let { println("editor null") }
        file as? TranscriptPsiFile ?: return let { println("file null") }
        if (!isRenaming(dataContext)) return let { println("not renaming") }
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

        private fun getEffectiveSelection(editor: Editor): ClosedIntRange {
            with(editor.selectionModel) {
                val start = max(blockSelectionStarts[0] - 1, 0)
                val end = min(blockSelectionEnds[0], editor.document.textLength - 1)
                return ClosedIntRange(start, end)
            }
        }

        fun getWord(editor: Editor, psiFile: TranscriptPsiFile): TranscriptWord? {
            val start = max(editor.caretModel.offset - 1, 0)
            val end = min(editor.caretModel.offset, editor.document.textLength - 1)
            val range = ClosedIntRange(start, end)
            val elementBounds = getElementBounds(range, psiFile) ?: return null
            return wordsBetween(elementBounds.first, elementBounds.second).firstOrNull()
        }

        fun getSelectedWords(editor: Editor, psiFile: TranscriptPsiFile): List<TranscriptWord> {
            val selection = getEffectiveSelection(editor)
            val elementBounds = getElementBounds(selection, psiFile) ?: return listOf()
            return wordsBetween(elementBounds.first, elementBounds.second).toList()
        }
    }
}