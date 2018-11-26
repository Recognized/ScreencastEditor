package vladsaif.syncedit.plugin.util

import java.util.*
import kotlin.math.max
import kotlin.math.min

val LongRange.empty: Boolean
  get() = start > endInclusive
val LongRange.Companion.INTERSECTS_CMP get() = INTERSECTS_CMP_LONG
inline val LongRange.end get() = endInclusive
inline val LongRange.length: Long
  get() = max(endInclusive - start + 1, 0L)

fun LongRange.intersects(other: LongRange): Boolean {
  if (other.empty || empty) return false
  return !(start > other.endInclusive || other.start > endInclusive)
}

fun LongRange.shift(delta: Long): LongRange {
  return start + delta..endInclusive + delta
}

fun LongRange.mapLong(fn: (Long) -> Long): LongRange = fn(start)..fn(endInclusive)

fun LongRange.mapInt(fn: (Long) -> Int): IntRange = fn(start)..fn(endInclusive)

fun LongRange.compareTo(other: LongRange): Int {
  return (start - other.start).floorToInt()
}

fun LongRange.Companion.from(startOffsetMs: Long, length: Long): LongRange {
  return LongRange(startOffsetMs, startOffsetMs + length - 1)
}

fun Long.floorToInt(): Int {
  return if (this > 0) min(this, Int.MAX_VALUE.toLong()).toInt()
  else max(this, Int.MIN_VALUE.toLong()).toInt()
}

infix fun LongRange.intersectWith(other: LongRange): LongRange {
  return if (intersects(other)) LongRange(max(start, other.start), min(endInclusive, other.endInclusive))
  else LongRange.EMPTY
}

operator fun LongRange.contains(other: LongRange): Boolean {
  return start <= other.start && other.endInclusive <= endInclusive
}

private val INTERSECTS_CMP_LONG = Comparator<LongRange> { a, b ->
  return@Comparator if (a.intersects(b)) 0 else (a.start - b.start).floorToInt()
}
