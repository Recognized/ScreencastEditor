package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import vladsaif.syncedit.plugin.ClosedIntRange
import kotlin.coroutines.experimental.buildSequence

fun wordsBetween(start: PsiElement, end: PsiElement) = buildSequence<TranscriptWord> {
    var x: PsiElement? = start
    while (true) {
        if (x is TranscriptWord) {
            yield(x)
        }
        if (x == end || x == null) {
            break
        }
        x = x.nextSibling
    }
}

fun getElementBounds(textRange: ClosedIntRange, psiFile: TranscriptPsiFile): Pair<PsiElement, PsiElement>? {
    val startElement = psiFile.findElementAt(textRange.start).let {
        it as? PsiWhiteSpace ?: it?.parent
    }
    val endElement = psiFile.findElementAt(textRange.end).let {
        it as? PsiWhiteSpace ?: it?.parent
    }
    if (startElement == null || endElement == null) return null
    return startElement to endElement
}