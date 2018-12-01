package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import vladsaif.syncedit.plugin.model.WordData

interface TranscriptWord : PsiNameIdentifierOwner {
  val number: Int
  val data: WordData?
}

internal class TranscriptWordImpl(node: ASTNode) : ASTWrapperPsiElement(node), TranscriptWord {
  override val data
    get() = (parent as TranscriptPsiFile).audio?.data?.words?.get(number)
  override val number: Int
    get() {
      var j = 0
      for (word in wordsBetween(parent.firstChild, parent.lastChild)) {
        if (word == this) return j
        ++j
      }
      return -1
    }

  override fun getNameIdentifier() = this

  override fun setName(name: String): PsiElement {
    containingFile
    return this
  }

  override fun toString(): String = text
}