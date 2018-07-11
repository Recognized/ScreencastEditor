package vladsaif.syncedit.plugin

import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Represents time range with included ends
 */
data class ClosedIntRange(val start: Int, val end: Int) : Comparable<ClosedIntRange> {

    val length: Int
        get() = max(end - start + 1, 0)

    val empty: Boolean
        get() = start > end

    operator fun contains(other: ClosedIntRange): Boolean {
        return start <= other.start && other.end <= end
    }

    operator fun contains(other: Int): Boolean {
        return other in start..end
    }

    infix fun shift(delta: Int): ClosedIntRange {
        if (delta > 0 && end > Int.MAX_VALUE - delta || delta < 0 && start < Int.MIN_VALUE - delta) {
            throw IllegalArgumentException()
        }
        return ClosedIntRange(start + delta, end + delta)
    }

    fun stretchRight(delta: Int): ClosedIntRange {
        if (delta < 0 || Int.MAX_VALUE - end < delta) throw IllegalArgumentException()
        return ClosedIntRange(start, end + min(delta, Int.MAX_VALUE - end))
    }

    fun intersects(other: ClosedIntRange): Boolean {
        if (other.empty || empty) return false
        return !(start > other.end || other.start > end)
    }

    infix fun intersect(other: ClosedIntRange): ClosedIntRange {
        return if (intersects(other)) ClosedIntRange(max(start, other.start), min(end, other.end))
        else EMPTY_RANGE
    }

    override fun compareTo(other: ClosedIntRange): Int {
        return start - other.start
    }

    override fun toString(): String {
        return "[$start, $end]"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ClosedIntRange) return false
        if (empty && other.empty) return true
        return start == other.start && end == other.end
    }

    override fun hashCode(): Int {
        return if (empty) 0 else start + end
    }

    companion object {

        infix fun Int.clr(other: Int) = ClosedIntRange(this, other)

        val EMPTY_RANGE = ClosedIntRange(0, -1)

        fun from(startOffsetMs: Int, length: Int): ClosedIntRange {
            return ClosedIntRange(startOffsetMs, startOffsetMs + length - 1)
        }

        /**
         * Merges adjacent ranges into one.
         * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
         *
         * @param ranges the ranges to merge
         * @return new set with merged ranges
         */
        fun mergeAdjacent(ranges: Set<ClosedIntRange>): Set<ClosedIntRange> {
            return mergeAdjacent(ArrayList(ranges)).toSet()
        }

        /**
         * Merges adjacent ranges into one.
         * Two ranges are considered adjacent if `one.getEndOffset() + 1 >= another.getStartOffset()`.
         *
         * @param merge ranges to merge
         * @return new list with merged ranges
         */
        fun mergeAdjacent(merge: List<ClosedIntRange>): List<ClosedIntRange> {
            val list = merge.toMutableList().sorted()
            val result = mutableListOf<ClosedIntRange>()
            var start = -1
            for (i in list.indices) {
                if (i == 0 || list[i].start > list[i - 1].end + 1) {
                    if (start != -1) {
                        result.add(ClosedIntRange(start, list[i - 1].end))
                    }
                    start = list[i].start
                }
            }
            if (start != -1) {
                result.add(ClosedIntRange(start, list.last().end))
            }
            return result
        }
    }
}
