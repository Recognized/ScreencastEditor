package vladsaif.syncedit.plugin.lang.transcript.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import vladsaif.syncedit.plugin.WordData.State.EXCLUDED
import vladsaif.syncedit.plugin.WordData.State.MUTED
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class TranscriptAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is TranscriptWord) {
      when (element.data?.state) {
        EXCLUDED -> {
          val annotation = holder.createInfoAnnotation(element, "Word is excluded from transcript")
          annotation.textAttributes = Highlighters.EXCLUDED_WORD
        }
        MUTED -> {
          val annotation = holder.createInfoAnnotation(element, "Word is muted")
          annotation.textAttributes = Highlighters.MUTED_WORD
        }
        else -> Unit
      }
    }
  }
}