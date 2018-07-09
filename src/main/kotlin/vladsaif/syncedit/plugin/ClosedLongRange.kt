package vladsaif.syncedit.plugin

import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Represents time range with included ends
 */
data class ClosedLongRange(val start: Long, val end: Long) : Comparable<ClosedLongRange> {

    val length: Long
        get() = max(end - start + 1, 0)

    val empty: Boolean
        get() = start > end

    operator fun contains(other: ClosedLongRange): Boolean {
        return start <= other.start && other.end <= end
    }

    fun shift(delta: Int): ClosedLongRange {
        return ClosedLongRange(start + delta, end + delta)
    }

    fun stretchRight(delta: Int): ClosedLongRange {
        return ClosedLongRange(start, end + delta)
    }

    fun intersects(other: ClosedLongRange): Boolean {
        if (other.empty || empty) return false
        return !(start > other.end || other.start > end)
    }

    infix fun intersect(other: ClosedLongRange): ClosedLongRange {
        return if (intersects(other)) ClosedLongRange(max(start, other.start), min(end, other.end))
        else EMPTY_RANGE
    }

    override fun compareTo(other: ClosedLongRange): Int {
        return (start - other.start).toInt()
    }

    override fun toString(): String {
        return "[$start, $end]"
    }

    companion object {

        val EMPTY_RANGE = ClosedLongRange(0, -1)

        fun from(startOffsetMs: Long, length: Long): ClosedLongRange {
            return ClosedLongRange(startOffsetMs, startOffsetMs + length - 1)
        }

        fun intersectionLength(a: ClosedLongRange, b: ClosedLongRange): Long {
            if (a.empty || b.empty) return 0
            return Math.max(Math.min(a.end, b.end) - Math.max(a.start, b.start) + 1, 0)
        }

        /**
         * Merges adjacent ranges into one.
         * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
         *
         * @param ranges the ranges to merge
         * @return new set with merged ranges
         */
        fun mergeAdjacent(ranges: Set<ClosedLongRange>): Set<ClosedLongRange> {
            return mergeAdjacent(ArrayList(ranges)).toSet()
        }

        /**
         * Merges adjacent ranges into one.
         * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
         *
         * @param list ranges to merge
         * @return new list with merged ranges
         */
        fun mergeAdjacent(merge: List<ClosedLongRange>): List<ClosedLongRange> {
            val list = merge.toMutableList().sorted()
            val result = mutableListOf<ClosedLongRange>()
            var start = -1L
            for (i in list.indices) {
                if (i == 0 || list[i].start > list[i - 1].end + 1) {
                    if (start != -1L) {
                        result.add(ClosedLongRange(start, list[i - 1].end))
                    }
                    start = list[i].start
                }
            }
            if (start != -1L) {
                result.add(ClosedLongRange(start, list.last().end))
            }
            return result
        }
    }
}
