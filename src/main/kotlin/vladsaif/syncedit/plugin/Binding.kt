package vladsaif.syncedit.plugin

import com.intellij.openapi.editor.RangeMarker

data class Binding(
    val itemRange: IRange,
    val lineRange: IRange
)

fun mergeBindings(bindings: List<Binding>): List<Binding> {
  val sorted = bindings.filter { !it.itemRange.empty && !it.lineRange.empty }.sortedBy { it.itemRange }
  return sorted.fold(mutableListOf()) { acc, x ->
    when {
      x.itemRange.empty -> Unit
      acc.isEmpty() || acc.last().itemRange.end + 1 < x.itemRange.start || acc.last().lineRange != x.lineRange -> {
        acc.add(x)
      }
      else -> {
        acc[acc.lastIndex] = acc.last().copy(itemRange = acc.last().itemRange.copy(end = x.itemRange.end))
      }
    }
    acc
  }
}

fun createBindings(wordMapping: Map<Int, RangeMarker>, lineConverter: (IRange) -> IRange = { it }) = wordMapping.entries
    .map { (index, x) -> Binding(IRange(index, index), lineConverter(x.toLineRange())) }
    .let(::mergeBindings)

fun RangeMarker.toLineRange(): IRange {
  if (!isValid) return IRange.EMPTY_RANGE
  val doc = this.document
  return IRange(doc.getLineNumber(startOffset), doc.getLineNumber(endOffset))
}