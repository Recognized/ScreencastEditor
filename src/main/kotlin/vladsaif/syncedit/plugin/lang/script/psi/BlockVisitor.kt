package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

object BlockVisitor {

  fun visit(scriptFile: KtFile, consumer: (PsiElement) -> Unit) {
    if (!scriptFile.isScript()) throw IllegalArgumentException("Not a script file: $scriptFile (${scriptFile.language})")
    visitStatements(scriptFile.script!!.blockExpression.statements, consumer)
  }

  private fun visitStatements(statements: List<KtExpression>, consumer: (PsiElement) -> Unit) {
    for (x in statements.filter { it !is KtProperty && it !is KtClassOrObject && it !is KtFunction }) {
      consumer(x)
      visitBlocks(x, consumer)
    }
  }

  private fun visitBlocks(element: PsiElement, consumer: (PsiElement) -> Unit) {
    if (element is KtBlockExpression) {
      visitStatements(element.statements, consumer)
      return
    }
    for (x in element.children) {
      visitBlocks(x, consumer)
    }
  }
}