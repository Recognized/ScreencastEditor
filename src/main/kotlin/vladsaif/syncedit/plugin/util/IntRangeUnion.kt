package vladsaif.syncedit.plugin.util

import kotlin.math.max
import kotlin.math.min

class IntRangeUnion {
  private var myLastCalculated: IntRange? = null
  private var myCursor = 0
  private var myCursorValue = 0
  /**
   * Invariant: sorted in ascending order, distance between each other at least one
   */
  private val myRanges = mutableListOf<IntRange>()
  val ranges
    get() = myRanges.toList()

  fun clear() {
    myRanges.clear()
    myLastCalculated = null
  }

  operator fun contains(other: IntRange): Boolean {
    if (other.empty) return true
    val startPos = myRanges.binarySearch(IntRange.from(other.start, 1), IntRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(IntRange.from(other.end, 1), IntRange.INTERSECTS_CMP)
    return startPos >= 0 && startPos == endPos
  }

  operator fun contains(other: Int): Boolean {
    return IntRange(other, other) in this
  }

  fun exclude(range: IntRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(IntRange.from(range.start, 1), IntRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(IntRange.from(range.end, 1), IntRange.INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      // Add piece of start if it was replaced
      if (oldStart < range.start) {
        toEdit.add(IntRange(oldStart, range.start - 1))
      }
      if (oldEnd > range.end) {
        toEdit.add(IntRange(range.end + 1, oldEnd))
      }
    }
  }

  fun intersection(range: IntRange): List<IntRange> {
    val ret = mutableListOf<IntRange>()
    for (x in myRanges) {
      val intersection = x intersectWith range
      if (!intersection.empty) {
        ret.add(intersection)
      }
    }
    return ret
  }

  fun union(range: IntRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(IntRange.from(range.start - 1, 1), IntRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(IntRange.from(range.end + 1, 1), IntRange.INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      toEdit.add(IntRange(min(oldStart, range.start), max(oldEnd, range.end)))
    } else {
      toEdit.add(range)
    }
  }

  fun impose(range: IntRange): IntRange {
    var accumulator = 0
    var i = 0
    if (myLastCalculated != null && myLastCalculated!!.start <= range.start) {
      i = myCursor
      accumulator = myCursorValue
    }
    while (i < myRanges.size && myRanges[i].end < range.start) {
      accumulator += myRanges[i].length
      ++i
    }
    myCursor = i
    myCursorValue = accumulator
    myLastCalculated = range
    var left = range.start - accumulator
    var right = range.end - accumulator
    val leftPart = IntRange(0, Math.max(range.start - 1, 0))
    val rightPart = IntRange(0, range.end)
    while (i < myRanges.size) {
      val cur = myRanges[i]
      if (cur.start <= range.end) {
        right -= rightPart.intersectWith(cur).length
        left -= leftPart.intersectWith(cur).length
      } else
        break
      ++i
    }
    return IntRange(left, right)
  }

  override fun toString(): String {
    return "ClosedIntRanges" + myRanges.joinToString(separator = ",", prefix = "(", postfix = ")")
  }

  override fun equals(other: Any?): Boolean {
    return other is IntRangeUnion && other.myRanges == myRanges
  }

  override fun hashCode(): Int {
    return myRanges.hashCode()
  }

  companion object {

    private fun toInsertPosition(x: Int): Int {
      return if (x < 0) -x - 1 else x
    }
  }
}