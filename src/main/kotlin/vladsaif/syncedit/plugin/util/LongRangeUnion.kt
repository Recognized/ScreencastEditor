package vladsaif.syncedit.plugin.util

import kotlin.math.max
import kotlin.math.min

class LongRangeUnion {
  private var myLastCalculated: LongRange? = null
  private var myCursor = 0
  private var myCursorValue = 0L
  /**
   * Invariant: sorted in ascending order, distance between each other at least one
   */
  private val myRanges = mutableListOf<LongRange>()
  val ranges
    get() = myRanges.toList()

  operator fun contains(other: LongRange): Boolean {
    if (other.empty) return true
    val startPos = myRanges.binarySearch(LongRange.from(other.start, 1), LongRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LongRange.from(other.end, 1), LongRange.INTERSECTS_CMP)
    return startPos >= 0 && startPos == endPos
  }

  fun exclude(range: LongRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(LongRange.from(range.start, 1), LongRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LongRange.from(range.end, 1), LongRange.INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      // Add piece of start if it was replaced
      if (oldStart < range.start) {
        toEdit.add(LongRange(oldStart, range.start - 1))
      }
      if (oldEnd > range.end) {
        toEdit.add(LongRange(range.end + 1, oldEnd))
      }
    }
  }

  fun intersection(range: LongRange): List<LongRange> {
    val ret = mutableListOf<LongRange>()
    for (x in myRanges) {
      val intersection = x intersectWith range
      if (!intersection.empty) {
        ret.add(intersection)
      }
    }
    return ret
  }


  fun union(range: LongRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(LongRange.from(range.start - 1, 1), LongRange.INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LongRange.from(range.end + 1, 1), LongRange.INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      toEdit.add(LongRange(min(oldStart, range.start), max(oldEnd, range.end)))
    } else {
      toEdit.add(range)
    }
  }

  fun impose(range: LongRange): LongRange {
    var accumulator = 0L
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
    val leftPart = LongRange(0, Math.max(range.start - 1, 0))
    val rightPart = LongRange(0, range.end)
    while (i < myRanges.size) {
      val cur = myRanges[i]
      if (cur.start <= range.end) {
        right -= rightPart.intersectWith(cur).length
        left -= leftPart.intersectWith(cur).length
      } else
        break
      ++i
    }
    return LongRange(left, right)
  }

  override fun toString(): String {
    return "ClosedLongRanges" + myRanges.joinToString(separator = ",", prefix = "(", postfix = ")")
  }

  override fun equals(other: Any?): Boolean {
    return other is LongRangeUnion && other.myRanges == myRanges
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