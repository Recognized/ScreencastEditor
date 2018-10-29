package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

object BlockVisitor {

  fun visit(scriptFile: KtFile, consumer: (PsiElement) -> Unit) {
    if (!scriptFile.isScript()) throw IllegalArgumentException("Not a script file: $scriptFile (${scriptFile.language})")
    visit(scriptFile.script!!.blockExpression.statements, consumer)
  }

  fun <T> fold(
    scriptFile: KtFile,
    blockOp: (PsiElement, List<T>, Boolean) -> T
  ): List<T> {
    if (!scriptFile.isScript()) throw IllegalArgumentException("Not a script file: $scriptFile (${scriptFile.language})")
    return foldStatements(scriptFile.script!!.blockExpression.statements, blockOp)
  }

  private fun <T> foldStatements(
    statements: List<KtExpression>,
    blockOp: (PsiElement, List<T>, Boolean) -> T
  ): MutableList<T> {
    val list = mutableListOf<T>()
    for (x in statements.filter { it !is KtProperty && it !is KtClassOrObject && it !is KtFunction }) {
      if (!TimeOffsetParser.isTimeOffset(x)) {
        val (res, isBlock) = foldStatements(x, blockOp)
        list.add(blockOp(x, res, isBlock))
      }
    }
    return list
  }

  private fun <T> foldStatements(
    element: PsiElement,
    blockOp: (PsiElement, List<T>, Boolean) -> T
  ): Pair<List<T>, Boolean> {
    return if (element is KtBlockExpression) {
      foldStatements(element.statements, blockOp) to true
    } else {
      val list = mutableListOf<T>()
      var isBlock = false
      for (x in element.children) {
        val (a, b) = foldStatements(x, blockOp)
        list.addAll(a)
        isBlock = isBlock or b
      }
      list to isBlock
    }
  }

  private fun visit(statements: List<KtExpression>, consumer: (PsiElement) -> Unit) {
    for (x in statements.filter { it !is KtProperty && it !is KtClassOrObject && it !is KtFunction }) {
      consumer(x)
      visitBlocks(x, consumer)
    }
  }

  private fun visitBlocks(element: PsiElement, consumer: (PsiElement) -> Unit) {
    if (element is KtBlockExpression) {
      visit(element.statements, consumer)
      return
    }
    for (x in element.children) {
      visitBlocks(x, consumer)
    }
  }
}