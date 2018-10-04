package vladsaif.syncedit.plugin

import com.intellij.openapi.editor.RangeMarker

data class MergedLineMapping(
    val itemRange: IRange,
    val lineRange: IRange
)

/**
 * Key: word index
 * Value: mapped line range
 */
typealias LineMapping = Map<Int, IRange>

/**
 * Key: word index
 * Value: mapped text range
 */
typealias TextRangeMapping = Map<Int, RangeMarker>

fun mergeLineMappings(mergedLineMappings: List<MergedLineMapping>): List<MergedLineMapping> {
  val sorted = mergedLineMappings.filter { !it.itemRange.empty && !it.lineRange.empty }.sortedBy { it.itemRange }
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

fun createMergedLineMappings(lineMapping: LineMapping, lineConverter: (IRange) -> IRange = { it }) = lineMapping.entries
    .map { (index, x) -> MergedLineMapping(IRange(index, index), lineConverter(x)) }
    .let(::mergeLineMappings)

fun RangeMarker.toLineRange(): IRange {
  if (!isValid) return IRange.EMPTY_RANGE
  val doc = this.document
  return IRange(doc.getLineNumber(startOffset), doc.getLineNumber(endOffset))
}