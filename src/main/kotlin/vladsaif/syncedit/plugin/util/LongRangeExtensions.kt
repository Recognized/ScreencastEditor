package vladsaif.syncedit.plugin.util

import java.util.*
import kotlin.math.max
import kotlin.math.min

inline val LongRange.end get() = endInclusive

inline val LongRange.length: Long
  get() = max(end - start + 1, 0L)

val LongRange.empty: Boolean
  get() = start > end

operator fun LongRange.contains(other: LongRange): Boolean {
  return start <= other.start && other.end <= end
}

fun LongRange.intersects(other: LongRange): Boolean {
  if (other.empty || empty) return false
  return !(start > other.end || other.start > end)
}

infix fun LongRange.intersect(other: LongRange): LongRange {
  return if (intersects(other)) LongRange(max(start, other.start), min(end, other.end))
  else LongRange.EMPTY
}

fun LongRange.compareTo(other: LongRange): Int {
  return (start - other.start).floorToInt()
}

private val INTERSECTS_CMP_LONG = Comparator<LongRange> { a, b ->
  return@Comparator if (a.intersects(b)) 0 else (a.start - b.start).floorToInt()
}

val LongRange.Companion.INTERSECTS_CMP get() = INTERSECTS_CMP_LONG

fun LongRange.Companion.from(startOffsetMs: Long, length: Long): LongRange {
  return LongRange(startOffsetMs, startOffsetMs + length - 1)
}

fun Long.floorToInt(): Int {
  return if (this > 0) min(this, Int.MAX_VALUE.toLong()).toInt()
  else max(this, Int.MIN_VALUE.toLong()).toInt()
}