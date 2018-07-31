package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile

abstract class IncludeExcludeActionBase : AnAction() {

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