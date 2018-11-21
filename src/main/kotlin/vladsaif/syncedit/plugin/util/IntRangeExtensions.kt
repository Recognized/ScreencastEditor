package vladsaif.syncedit.plugin.util

import kotlin.Comparator
import kotlin.math.max
import kotlin.math.min

val IntRange.Companion.INTERSECTS_CMP get() = INTERSECTS_CMP_INT

inline val IntRange.end get() = endInclusive

inline val IntRange.length: Int
  get() = max(end - start + 1, 0)

inline val IntRange.empty: Boolean
  get() = start > end

fun IntRange.mapLong(fn: (Int) -> Long): LongRange = fn(start)..fn(endInclusive)

fun IntRange.mapInt(fn: (Int) -> Int): IntRange = fn(start)..fn(endInclusive)

fun IntRange.shift(delta: Int) = (start + delta)..(endInclusive + delta)
fun IntRange.intersects(other: IntRange): Boolean {
  if (other.empty || empty) return false
  return !(start > other.end || other.start > end)
}

fun IntRange.inside(x: Int) = when {
  x in start..end -> x
  x < start -> start
  else -> end
}

fun IntRange.msToNs(): LongRange = (start * 1_000_000L)..(end * 1_000_000L)

fun IntRange.padded(padding: Int) = ((start + padding)..(end - padding)).let { if (it.empty) IntRange.EMPTY else it }

fun IntRange.copy(start: Int = this.start, end: Int = this.end): IntRange {
  return start..end
}

fun IntRange.Companion.from(startOffsetMs: Int, length: Int): IntRange {
  return IntRange(startOffsetMs, startOffsetMs + length - 1)
}

infix fun IntRange.intersectWith(other: IntRange): IntRange {
  return if (intersects(other)) IntRange(max(start, other.start), min(end, other.end))
  else IntRange.EMPTY
}

operator fun IntRange.component1(): Int = start

operator fun IntRange.component2(): Int = end

operator fun IntRange.compareTo(other: IntRange): Int {
  return start - other.start
}
operator fun IntRange.plus(other: IntRange): IntRange {
  if (empty || this in other) return other
  if (other.empty || other in this) return this
  return IntRange(min(start, other.start), max(end, other.end))
}

operator fun IntRange.contains(other: IntRange): Boolean {
  return start <= other.start && other.end <= end
}

private val INTERSECTS_CMP_INT = Comparator<IntRange> { a, b ->
  return@Comparator if (a.intersects(b)) 0 else a.start - b.start
}
