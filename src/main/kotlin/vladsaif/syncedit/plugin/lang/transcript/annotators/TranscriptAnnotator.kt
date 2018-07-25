package vladsaif.syncedit.plugin.lang.transcript.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class TranscriptAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is TranscriptWord) {
            if (element.hidden) {
                println("annotating")
                val annotation = holder.createInfoAnnotation(element, "Word is excluded from transcript")
                annotation.enforcedTextAttributes = Highlighters.HIDDEN_WORD.defaultAttributes
            }
        }
    }
}