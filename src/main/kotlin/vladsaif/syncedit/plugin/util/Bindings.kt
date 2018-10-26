package vladsaif.syncedit.plugin.util

import com.intellij.openapi.editor.RangeMarker

data class MergedLineMapping(
    val itemRange: IntRange,
    val lineRange: IntRange
)

/**
 * Key: word index
 * Value: mapped line range
 */
typealias LineMapping = Map<Int, IntRange>

/**
 * Key: word index
 * Value: mapped text range
 */
typealias TextRangeMapping = Map<Int, RangeMarker>

fun mergeLineMappings(mergedLineMappings: List<MergedLineMapping>): List<MergedLineMapping> {
  val sorted = mergedLineMappings.asSequence().filter { !it.itemRange.empty && !it.lineRange.empty }.sortedBy { it.itemRange.start }
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

fun createMergedLineMappings(lineMapping: LineMapping, lineConverter: (IntRange) -> IntRange = { it }) = lineMapping.entries
    .map { (index, x) -> MergedLineMapping(IntRange(index, index), lineConverter(x)) }
    .let(::mergeLineMappings)

fun RangeMarker.toLineRange(): IntRange {
  if (!isValid) return IntRange.EMPTY
  val doc = this.document
  return IntRange(doc.getLineNumber(startOffset), doc.getLineNumber(endOffset))
}