package vladsaif.syncedit.plugin

data class Binding(
    val itemRange: IRange,
    val lineRange: IRange
)

fun mergeBindings(bindings: List<Binding>): List<Binding> {
  val sorted = bindings.filter { !it.itemRange.empty && !it.lineRange.empty }.sortedBy { it.itemRange }
  return sorted.foldIndexed(mutableListOf()) { index, acc, x ->
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

fun List<Binding>.lines(): Iterable<Int> {
  return this.flatMap { it.lineRange.toIntRange() }
}