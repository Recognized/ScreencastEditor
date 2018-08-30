package vladsaif.syncedit.plugin

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import kotlin.Comparator
import kotlin.math.max
import kotlin.math.min

/**
 * Represents time range with included ends
 */
@XmlAccessorType(XmlAccessType.FIELD)
data class IRange(
    @field:XmlAttribute val start: Int,
    @field:XmlAttribute val end: Int
) : Comparable<IRange> {
  val length: Int
    get() = max(end - start + 1, 0)

  val empty: Boolean
    get() = start > end

  private constructor() : this(0, -1)

  operator fun contains(other: IRange): Boolean {
    return start <= other.start && other.end <= end
  }

  operator fun contains(other: Int): Boolean {
    return other in start..end
  }

  fun toIntRange(): IntRange {
    return start..end
  }

  infix fun shift(delta: Int): IRange {
    if (delta > 0 && end > Int.MAX_VALUE - delta || delta < 0 && start < Int.MIN_VALUE - delta) {
      throw IllegalArgumentException()
    }
    return IRange(start + delta, end + delta)
  }

  fun stretchRight(delta: Int): IRange {
    if (delta < 0 || Int.MAX_VALUE - end < delta) throw IllegalArgumentException()
    return IRange(start, end + min(delta, Int.MAX_VALUE - end))
  }

  fun intersects(other: IRange): Boolean {
    if (other.empty || empty) return false
    return !(start > other.end || other.start > end)
  }

  infix fun intersect(other: IRange): IRange {
    return if (intersects(other)) IRange(max(start, other.start), min(end, other.end))
    else EMPTY_RANGE
  }

  fun inside(x: Int) = when {
    x in start..end -> x
    x < start -> start
    else -> end
  }

  operator fun plus(other: IRange): IRange {
    if (empty || this in other) return other
    if (other.empty || other in this) return this
    return IRange(min(start, other.start), max(end, other.end))
  }

  override fun compareTo(other: IRange): Int {
    return start - other.start
  }

  override fun toString(): String {
    return "[$start, $end]"
  }

  override fun equals(other: Any?): Boolean {
    if (other !is IRange) return false
    if (empty && other.empty) return true
    return start == other.start && end == other.end
  }

  override fun hashCode(): Int {
    return if (empty) 0 else start + end
  }

  companion object {

    val INTERSECTS_CMP = Comparator<IRange> { a, b ->
      return@Comparator if (a.intersects(b)) 0 else a.start - b.start
    }

    infix fun Int.clr(other: Int) = IRange(this, other)

    val EMPTY_RANGE = IRange(0, -1)

    fun from(startOffsetMs: Int, length: Int): IRange {
      return IRange(startOffsetMs, startOffsetMs + length - 1)
    }
  }
}

/**
 * @param left sorted in ascending order by key extracted with [leftExtractor]
 * @param right sorted in ascending order by key extracted with [rightExtractor]
 * @param consumer takes element from [left] list and most intersecting range of elements from right
 */
//fun intersect(
//    left: List<IRange>,
//    right: List<IRange>,
//    consumer: (IRange, IRange) -> Unit
//) {
//  assert(left.zipWithNext().all { (prev, next) -> prev <= next })
//  assert(right.zipWithNext().all { (prev, next) -> prev <= next })
//  var searchHint = 0
//  for (element in left) {
//    var j = searchHint
//    var intersection = IRange.EMPTY_RANGE
//    out@ while (j < right.size) {
//      val arg = right[j]
//      val cmp = IRange.INTERSECTS_CMP.compare(element, arg)
//      when {
//        cmp == 0 -> {
//          if (intersection.empty) {
//            searchHint = j
//          }
//          intersection += arg
//        }
//        cmp < 0 -> {
//          if (intersection.empty) {
//            searchHint = j
//          }
//          break@out
//        }
//        cmp > 0 -> {
//          searchHint = j
//        }
//      }
//      j++
//    }
//    consumer(element, intersection)
//  }
//}
//
//fun intersect(left: List<IRange>, right: List<IRange>): Map<IRange, IRange> {
//  val map = mutableMapOf<IRange, IRange>()
//  intersect(left, right) { a, b ->
//    if (!b.empty) map[a] = b
//  }
//  return map
//}
