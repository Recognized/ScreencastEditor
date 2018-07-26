package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.psi.PsiElement
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