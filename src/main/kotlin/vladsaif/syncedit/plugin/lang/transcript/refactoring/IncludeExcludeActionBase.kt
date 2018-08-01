package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.TranscriptModel
import vladsaif.syncedit.plugin.TranscriptModelUndoableAction
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord
import vladsaif.syncedit.plugin.lang.transcript.psi.getSelectedWords

abstract class IncludeExcludeActionBase : AnAction() {

    abstract fun doAction(model: TranscriptModel, words: List<TranscriptWord>)

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as TranscriptPsiFile
        val model = psi.model ?: return
        CommandProcessor.getInstance().executeCommand(project, {
            val currentData = model.data
            doAction(model, getSelectedWords(editor, psi))
            val newData = model.data
            val undo = TranscriptModelUndoableAction(model, currentData, newData)
            undo.addAffectedDocuments(DocumentReferenceManager.getInstance().create(editor.document))
            undo.addAffectedDocuments(DocumentReferenceManager.getInstance().create(model.xmlFile))
            UndoManager.getInstance(project).undoableActionPerformed(undo)
        }, this.javaClass.simpleName, "ScreencastEditor", editor.document)
    }

    override fun update(e: AnActionEvent?) {
        e ?: return
        val context = e.dataContext
        val project = context.getData(CommonDataKeys.PROJECT)
        val editor = context.getData(CommonDataKeys.EDITOR)
        if (project == null || editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        e.presentation.isEnabledAndVisible = psi is TranscriptPsiFile
                && editor.caretModel.offset >= 0
    }
}