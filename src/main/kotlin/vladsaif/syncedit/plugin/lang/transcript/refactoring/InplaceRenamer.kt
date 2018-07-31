package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.codeInsight.daemon.impl.quickfix.EmptyExpression
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.template.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.RefactoringBundle
import com.intellij.util.containers.Stack
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class InplaceRenamer(val editor: Editor, private val word: TranscriptWord) {
    private val myHighlighters = mutableListOf<RangeHighlighter>()
    private val psiFile: TranscriptPsiFile?
        get() {
            val manager = PsiDocumentManager.getInstance(editor.project ?: return null)
            return manager.getPsiFile(editor.document) as? TranscriptPsiFile
        }
    private var originalIndex = word.number
    private var originalWord = word.text

    private fun finishEditing() {
        activeRenamers.pop()
        val project = editor.project
        if (project != null && !project.isDisposed) {
            val highlightManager = HighlightManager.getInstance(project)
            for (highlighter in myHighlighters) {
                highlightManager.removeSegmentHighlighter(editor, highlighter)
            }
        }
        with(editor.document) {
            getUserData(GUARDED_BLOCKS)?.forEach { removeGuardedBlock(it) }
            val marker = createGuardedBlock(0, textLength).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }
            putUserData(GUARDED_BLOCKS, listOf(marker))
        }
    }

    fun cancel() {
        val highlighter = myHighlighters[0]
        ApplicationManager.getApplication().runWriteAction {
            editor.document.replaceString(highlighter.startOffset, highlighter.endOffset, originalWord)
        }
        finishEditing()
    }

    fun acceptTemplate() {
        val highlighter = myHighlighters[0]
        if (highlighter.startOffset == highlighter.endOffset) {
            psiFile?.model?.hideWord(originalIndex)
            cancel()
        } else {
            val textRange = TextRange(highlighter.startOffset, highlighter.endOffset)
            psiFile?.model?.renameWord(originalIndex, editor.document.getText(textRange))
            finishEditing()
        }
    }

    fun rename() {
        val project = editor.project ?: return
        myHighlighters.clear()

        val range = word.textRange

        allowEditions(range)

        CommandProcessor.getInstance().executeCommand(editor.project, {
            ApplicationManager.getApplication().runWriteAction {
                val offset = editor.caretModel.offset
                editor.caretModel.moveToOffset(word.textOffset)

                val template = buildTemplate(word)

                TemplateManager.getInstance(project).startTemplate(editor, template, object : TemplateEditingAdapter() {
                    override fun templateFinished(template: Template?, brokenOff: Boolean) {
                        acceptTemplate()
                    }

                    override fun templateCancelled(template: Template?) {
                        cancel()
                    }
                }) { _, value ->
                    value.isEmpty() || value[value.length - 1] != '\u00A0'
                }

                // restore old offset
                editor.caretModel.moveToOffset(offset)

                addHighlight(range, editor, myHighlighters)
                LOG.assertTrue(myHighlighters.size == 1)
            }
        }, RefactoringBundle.message("rename.title"), null)
    }

    private fun allowEditions(textRange: TextRange) {
        with(editor.document) {
            getUserData(GUARDED_BLOCKS)?.forEach { removeGuardedBlock(it) }
            val newBlocks = mutableListOf<RangeMarker>()
            if (textRange.startOffset > 0) {
                newBlocks.add(createGuardedBlock(0, textRange.startOffset).apply {
                    isGreedyToLeft = false
                    isGreedyToRight = false
                })
            }
            if (textRange.endOffset < textLength) {
                newBlocks.add(createGuardedBlock(textRange.endOffset, textLength).apply {
                    isGreedyToLeft = false
                    isGreedyToRight = false
                })
            }
            putUserData(GUARDED_BLOCKS, newBlocks.toList())
        }
    }

    companion object {
        private val LOG = logger<InplaceRenamer>()
        private val activeRenamers = Stack<InplaceRenamer>()
        val GUARDED_BLOCKS: Key<List<RangeMarker>> = Key.create("GUARDER_BLOCKS")

        fun rename(editor: Editor, word: TranscriptWord) {
            if (!activeRenamers.isEmpty()) {
                activeRenamers.peek().finishEditing()
            }
            val renamer = InplaceRenamer(editor, word)
            activeRenamers.push(renamer)
            renamer.rename()
        }

        private fun addHighlight(range: TextRange, editor: Editor, highlighters: List<RangeHighlighter>) {
            val colorsManager = EditorColorsManager.getInstance()
            val attributes = colorsManager.globalScheme.getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES)

            val highlightManager = HighlightManager.getInstance(editor.project ?: return)
            highlightManager.addOccurrenceHighlight(editor, range.startOffset, range.endOffset, attributes, 0, highlighters, null)
            for (highlighter in highlighters) {
                highlighter.isGreedyToLeft = true
                highlighter.isGreedyToRight = true
            }
        }

        private fun buildTemplate(word: TranscriptWord): Template {
            val builder = TemplateBuilderImpl(word)

            val node = word.node
            builder.replaceElement(word, "Your word", object : EmptyExpression() {
                override fun calculateQuickResult(context: ExpressionContext?): Result? {
                    return TextResult(node.text)
                }

                override fun calculateResult(context: ExpressionContext?): Result? {
                    return TextResult(node.text)
                }
            }, true)

            return builder.buildInlineTemplate()
        }
    }
}