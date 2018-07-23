package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

private val logger = logger<TranscriptRenameHandler>()
class TranscriptRenameHandler : RenameHandler {
    override fun isRenaming(dataContext: DataContext?) = isAvailableOnDataContext(dataContext)

    override fun isAvailableOnDataContext(dataContext: DataContext?): Boolean {
        dataContext ?: return false
        val editor = PlatformDataKeys.FILE_EDITOR.getData(dataContext) ?: return false
        logger.info("Editor: $editor")
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return false
        logger.info("Caret: $caret")
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false
        psiFile.findElementAt(caret.offset) as? TranscriptWord ?: return false
        return true;
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        TODO("not implemented")
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        TODO("not implemented")
    }
}