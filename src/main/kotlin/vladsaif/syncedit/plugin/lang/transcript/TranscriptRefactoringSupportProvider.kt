package vladsaif.syncedit.plugin.lang.transcript

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class TranscriptRefactoringSupportProvider : RefactoringSupportProvider() {

    override fun isSafeDeleteAvailable(element: PsiElement) = true

}