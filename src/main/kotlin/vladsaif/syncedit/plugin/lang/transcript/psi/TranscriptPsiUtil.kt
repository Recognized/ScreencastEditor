package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import vladsaif.syncedit.plugin.util.end
import kotlin.math.max
import kotlin.math.min

fun wordsBetween(start: PsiElement, end: PsiElement) = sequence<TranscriptWord> {
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

fun getElementBounds(textRange: IntRange, psiFile: TranscriptPsiFile): Pair<PsiElement, PsiElement>? {
  val startElement = psiFile.findElementAt(textRange.start).let {
    it as? PsiWhiteSpace ?: it?.parent
  }
  val endElement = psiFile.findElementAt(textRange.end).let {
    it as? PsiWhiteSpace ?: it?.parent
  }
  if (startElement == null || endElement == null) return null
  return startElement to endElement
}

fun getEffectiveSelection(editor: Editor): IntRange {
  with(editor.selectionModel) {
    val start = max(blockSelectionStarts[0] - 1, 0)
    val end = min(blockSelectionEnds[0], editor.document.textLength - 1)
    return IntRange(start, end)
  }
}

fun getSelectedWords(editor: Editor, psiFile: TranscriptPsiFile): List<TranscriptWord> {
  val selection = getEffectiveSelection(editor)
  val elementBounds = getElementBounds(selection, psiFile) ?: return listOf()
  return wordsBetween(elementBounds.first, elementBounds.second).toList()
}

fun getWord(editor: Editor, psiFile: TranscriptPsiFile): TranscriptWord? {
  val start = max(editor.caretModel.offset - 1, 0)
  val end = min(editor.caretModel.offset, editor.document.textLength - 1)
  val range = IntRange(start, end)
  val elementBounds = getElementBounds(range, psiFile) ?: return null
  return wordsBetween(elementBounds.first, elementBounds.second).firstOrNull()
}