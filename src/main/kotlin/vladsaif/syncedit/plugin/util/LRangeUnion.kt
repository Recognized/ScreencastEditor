package vladsaif.syncedit.plugin.util

import vladsaif.syncedit.plugin.util.LRange.Companion.INTERSECTS_CMP
import kotlin.math.max
import kotlin.math.min

class LRangeUnion {
  private var myLastCalculated: LRange? = null
  private var myCursor = 0
  private var myCursorValue = 0L
  /**
   * Invariant: sorted in ascending order, distance between each other at least one
   */
  private val myRanges = mutableListOf<LRange>()
  val ranges
    get() = myRanges.toList()

  operator fun contains(other: LRange): Boolean {
    if (other.empty) return true
    val startPos = myRanges.binarySearch(LRange.from(other.start, 1), INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LRange.from(other.end, 1), INTERSECTS_CMP)
    return startPos >= 0 && startPos == endPos
  }

  fun exclude(range: LRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(LRange.from(range.start, 1), INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LRange.from(range.end, 1), INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      // Add piece of start if it was replaced
      if (oldStart < range.start) {
        toEdit.add(LRange(oldStart, range.start - 1))
      }
      if (oldEnd > range.end) {
        toEdit.add(LRange(range.end + 1, oldEnd))
      }
    }
  }

  fun intersection(range: LRange): List<LRange> {
    val ret = mutableListOf<LRange>()
    for (x in myRanges) {
      val intersection = x intersect range
      if (!intersection.empty) {
        ret.add(intersection)
      }
    }
    return ret
  }


  fun union(range: LRange) {
    if (range.empty) return
    myLastCalculated = null
    val startPos = myRanges.binarySearch(LRange.from(range.start - 1, 1), INTERSECTS_CMP)
    val endPos = myRanges.binarySearch(LRange.from(range.end + 1, 1), INTERSECTS_CMP)
    val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
    val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
    if (!toEdit.isEmpty()) {
      val oldStart = toEdit[0].start
      val oldEnd = toEdit.last().end
      // Clear all touched
      toEdit.clear()
      toEdit.add(LRange(min(oldStart, range.start), max(oldEnd, range.end)))
    } else {
      toEdit.add(range)
    }
  }

  fun impose(range: LRange): LRange {
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
    val leftPart = LRange(0, Math.max(range.start - 1, 0))
    val rightPart = LRange(0, range.end)
    while (i < myRanges.size) {
      val cur = myRanges[i]
      if (cur.start <= range.end) {
        right -= rightPart.intersect(cur).length
        left -= leftPart.intersect(cur).length
      } else
        break
      ++i
    }
    return LRange(left, right)
  }

  override fun toString(): String {
    return "ClosedLongRanges" + myRanges.joinToString(separator = ",", prefix = "(", postfix = ")")
  }

  override fun equals(other: Any?): Boolean {
    return other is LRangeUnion && other.myRanges == myRanges
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