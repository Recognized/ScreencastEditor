package vladsaif.syncedit.plugin

import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Represents time range with included ends
 */
data class LRange(val start: Long, val end: Long) : Comparable<LRange> {

  val length: Long
    get() = max(end - start + 1, 0L)

  val empty: Boolean
    get() = start > end

  operator fun contains(other: LRange): Boolean {
    return start <= other.start && other.end <= end
  }

  operator fun contains(other: Long): Boolean {
    return other in start..end
  }

  infix fun shift(delta: Long): LRange {
    if (delta > 0L && end > Long.MAX_VALUE - delta || delta < 0L && start < Long.MIN_VALUE - delta) {
      throw IllegalArgumentException()
    }
    return LRange(start + delta, end + delta)
  }

  fun stretchRight(delta: Long): LRange {
    if (delta < 0L || Long.MAX_VALUE - end < delta) throw IllegalArgumentException()
    return LRange(start, end + min(delta, Long.MAX_VALUE - end))
  }

  fun intersects(other: LRange): Boolean {
    if (other.empty || empty) return false
    return !(start > other.end || other.start > end)
  }

  infix fun intersect(other: LRange): LRange {
    return if (intersects(other)) LRange(max(start, other.start), min(end, other.end))
    else EMPTY_RANGE
  }

  override fun compareTo(other: LRange): Int {
    return (start - other.start).floorToInt()
  }

  override fun toString(): String {
    return "[$start, $end]"
  }

  override fun equals(other: Any?): Boolean {
    if (other !is LRange) return false
    if (empty && other.empty) return true
    return start == other.start && end == other.end
  }

  override fun hashCode(): Int {
    return if (empty) 0 else (start + end).toInt()
  }

  companion object {

    val INTERSECTS_CMP = Comparator<LRange> { a, b ->
      return@Comparator if (a.intersects(b)) 0 else (a.start - b.start).floorToInt()
    }

    infix fun Long.clr(other: Long) = LRange(this, other)

    val EMPTY_RANGE = LRange(0, -1)

    fun from(startOffsetMs: Long, length: Long): LRange {
      return LRange(startOffsetMs, startOffsetMs + length - 1)
    }

    /**
     * Merges adjacent ranges into one.
     * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
     *
     * @param ranges the ranges to merge
     * @return new set with merged ranges
     */
    fun mergeAdjacent(ranges: Set<LRange>): Set<LRange> {
      return mergeAdjacent(ArrayList(ranges)).toSet()
    }

    /**
     * Merges adjacent ranges into one.
     * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
     *
     * @param merge ranges to merge
     * @return new list with merged ranges
     */
    fun mergeAdjacent(merge: List<LRange>): List<LRange> {
      val list = merge.toMutableList().sorted()
      val result = mutableListOf<LRange>()
      var start = -1L
      for (i in list.indices) {
        if (i == 0 || list[i].start > list[i - 1].end + 1) {
          if (start != -1L) {
            result.add(LRange(start, list[i - 1].end))
          }
          start = list[i].start
        }
      }
      if (start != -1L) {
        result.add(LRange(start, list.last().end))
      }
      return result
    }
  }
}

fun Long.floorToInt(): Int {
  return if (this > 0) min(this, Int.MAX_VALUE.toLong()).toInt()
  else max(this, Int.MIN_VALUE.toLong()).toInt()
}