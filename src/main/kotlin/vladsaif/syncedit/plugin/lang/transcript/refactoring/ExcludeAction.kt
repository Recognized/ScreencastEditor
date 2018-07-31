package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.lang.transcript.psi.getSelectedWords

class ExcludeAction : IncludeExcludeActionBase() {

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as TranscriptPsiFile
        psi.model?.hideWords(getSelectedWords(editor, psi).map { it.number }.toIntArray())
    }
}
