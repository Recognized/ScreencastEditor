package vladsaif.syncedit.plugin.lang.transcript.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

class TRefactoringSupportProvider : RefactoringSupportProvider() {

    override fun isSafeDeleteAvailable(element: PsiElement) = false

}