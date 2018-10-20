package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiElement

data class TimedPsiElement(val element: PsiElement, val time: IntRange) : Comparable<TimedPsiElement> {
  override fun compareTo(other: TimedPsiElement) = CMP.compare(this, other)

  companion object {
    private val CMP = compareBy<TimedPsiElement> { it.time.start }
  }
}

