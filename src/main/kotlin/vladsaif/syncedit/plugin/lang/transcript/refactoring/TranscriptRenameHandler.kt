package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.refactoring.rename.RenameHandler
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.max

private val logger = logger<TranscriptRenameHandler>()

class TranscriptRenameHandler : RenameHandler {
    override fun isRenaming(dataContext: DataContext?) = isAvailableOnDataContext(dataContext)

    override fun isAvailableOnDataContext(dataContext: DataContext?): Boolean {
        dataContext ?: return false
        val editor = getEditor(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) as? TranscriptPsiFile ?: return false
        return isSelectionContainsWords(getEffectiveSelection(editor), file)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        editor ?: return let { println("editor null") }
        file as? TranscriptPsiFile ?: return let { println("file null") }
        if (!isRenaming(dataContext)) return let { println("not renaming") }
        val word = getWord(editor, file)
        if (word != null) {
            TranscriptInplaceRenamer.rename(editor, word)
        } else {
            val otherWord = getSelectedWords(editor, file).firstOrNull() ?: return
            TranscriptInplaceRenamer.rename(editor, otherWord)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        throw UnsupportedOperationException()
    }

    companion object {

        private fun getEditor(context: DataContext?): Editor? {
            return CommonDataKeys.EDITOR.getData(context ?: return null)
        }

        private fun isSelectionContainsWords(selectionRange: ClosedIntRange, psiFile: TranscriptPsiFile): Boolean {
            val bounds = getElementBounds(selectionRange, psiFile)
            if (bounds != null) {
                return getWordsBetween(bounds.first, bounds.second).any()
            }
            return false
        }

        private fun getWordsBetween(start: PsiElement, end: PsiElement) = buildSequence<TranscriptWord> {
            var x: PsiElement? = start
            while (true) {
                if (x is TranscriptWord) {
                    yield(x)
                }
                if (x == end || x == null) {
                    break
                }
                x = x.nextSibling
            }
        }

        private fun getElementBounds(textRange: ClosedIntRange, psiFile: TranscriptPsiFile): Pair<PsiElement, PsiElement>? {
            val startElement = psiFile.findElementAt(textRange.start).let {
                it as? PsiWhiteSpace ?: it?.parent
            }
            val endElement = psiFile.findElementAt(textRange.end).let {
                it as? PsiWhiteSpace ?: it?.parent
            }
            if (startElement == null || endElement == null) return null
            return startElement to endElement
        }

        private fun getEffectiveSelection(editor: Editor): ClosedIntRange {
            with(editor.selectionModel) {
                val start = max(blockSelectionStarts[0] - 1, 0)
                val end = blockSelectionEnds[0]
                return ClosedIntRange(start, end)
            }
        }

        fun getWord(editor: Editor, psiFile: TranscriptPsiFile): TranscriptWord? {
            val start = editor.caretModel.offset - 1
            val end = editor.caretModel.offset
            val range = ClosedIntRange(start, end)
            val elementBounds = getElementBounds(range, psiFile) ?: return null
            return getWordsBetween(elementBounds.first, elementBounds.second).firstOrNull()
        }

        fun getSelectedWords(editor: Editor, psiFile: TranscriptPsiFile): List<TranscriptWord> {
            val selection = getEffectiveSelection(editor)
            val elementBounds = getElementBounds(selection, psiFile) ?: return listOf()
            return getWordsBetween(elementBounds.first, elementBounds.second).toList()
        }
    }
}