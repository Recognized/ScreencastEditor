package vladsaif.syncedit.plugin

import java.util.*
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
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
 * @param Left type that has full order
 * @param Right type that has full order
 * @param left sorted in ascending order
 * @param right sorted in ascending order
 * @param isIntersects function that says if both it arguments are intersecting
 * @param consumer takes element from [left] list and most intersecting range of elements from right
 */
fun <Left : Comparable<Left>, Right : Comparable<Right>> intersect(
    left: List<Left>,
    right: List<Right>,
    isIntersects: (Left, Right) -> Boolean,
    consumer: (Left, Pair<Right, Right>?) -> Unit
) {
  assert(left.zipWithNext().all { (first, next) -> first <= next })
  assert(right.zipWithNext().all { (first, next) -> first <= next })
  var searchHint = 0
  for (element in left) {
    var j = searchHint
    var startIntersection: Right? = null
    var endIntersection: Right? = null
    searchHint = right.size
    while (j < right.size) {
      val arg = right[j]
      if (isIntersects(element, arg)) {
        if (startIntersection == null) {
          searchHint = j
          startIntersection = arg
        }
        endIntersection = arg
      } else if (startIntersection != null) {
        break
      }
      j++
    }
    consumer(
        element,
        if (startIntersection == null || endIntersection == null) null
        else startIntersection to endIntersection
    )
  }
}

fun <Left : Comparable<Left>, Right : Comparable<Right>> intersect(
    left: List<Left>,
    right: List<Right>,
    isIntersects: (Left, Right) -> Boolean
): Map<Left, Pair<Right, Right>> {
  val map = mutableMapOf<Left, Pair<Right, Right>>()
  intersect(left, right, isIntersects) { x, y ->
    if (y != null) map[x] = y
  }
  return map
}
