package vladsaif.syncedit.plugin

class ClosedIntRangeUnion {

    private var lastCalculated: ClosedIntRange? = null
    private var cachedIndex = 0
    private var cachedAccum = 0
    /**
     * Invariant: sorted in ascending order, distance between each other at least one
     */
    private val myRanges = mutableListOf<ClosedIntRange>()
    val ranges
        get() = myRanges.toList()

    fun clear() {
        myRanges.clear()
        lastCalculated = null
    }

    fun load(other: ClosedIntRangeUnion) {
        clear()
        myRanges.addAll(other.myRanges)
    }

    fun copy(): ClosedIntRangeUnion {
        val union = ClosedIntRangeUnion()
        union.load(this)
        return union
    }

    operator fun contains(other: ClosedIntRange): Boolean {
        if (other.empty) return true
        val startPos = myRanges.binarySearch(ClosedIntRange.from(other.start, 1), INTERSECTS_CMP)
        val endPos = myRanges.binarySearch(ClosedIntRange.from(other.end, 1), INTERSECTS_CMP)
        return startPos >= 0 && startPos == endPos
    }

    fun exclude(range: ClosedIntRange) {
        if (range.empty) return
        lastCalculated = null
        val startPos = myRanges.binarySearch(ClosedIntRange.from(range.start, 1), INTERSECTS_CMP)
        val endPos = myRanges.binarySearch(ClosedIntRange.from(range.end, 1), INTERSECTS_CMP)
        val lastTouched = if (endPos < 0) toInsertPosition(endPos) - 1 else endPos
        val toEdit = myRanges.subList(toInsertPosition(startPos), lastTouched + 1)
        if (!toEdit.isEmpty()) {
            val oldStart = toEdit[0].start
            val oldEnd = toEdit.last().end
            // Clear all touched
            toEdit.clear()
            // Add piece of start if it was replaced
            if (oldStart < range.start) {
                toEdit.add(ClosedIntRange(oldStart, range.start - 1))
            }
            if (oldEnd > range.end) {
                toEdit.add(ClosedIntRange(range.end + 1, oldEnd))
            }
        }
    }

    fun union(range: ClosedIntRange) {
        if (range.empty) return
        lastCalculated = null
        myRanges.add(range)
        val merged = ClosedIntRange.mergeAdjacent(myRanges)
        myRanges.clear()
        myRanges.addAll(merged)
    }

    fun impose(range: ClosedIntRange): ClosedIntRange {
        var accumulator = 0
        var i = 0
        if (lastCalculated != null && lastCalculated!!.start <= range.start) {
            i = cachedIndex
            accumulator = cachedAccum
        }
        while (i < myRanges.size && myRanges[i].end < range.start) {
            accumulator += myRanges[i].length
            ++i
        }
        cachedIndex = i
        cachedAccum = accumulator
        lastCalculated = range
        var left = range.start - accumulator
        var right = range.end - accumulator
        val leftPart = ClosedIntRange(0, Math.max(range.start - 1, 0))
        val rightPart = ClosedIntRange(0, range.end)
        while (i < myRanges.size) {
            val cur = myRanges[i]
            if (cur.start <= range.end) {
                right -= rightPart.intersect(cur).length
                left -= leftPart.intersect(cur).length
            } else
                break
            ++i
        }
        return ClosedIntRange(left, right)
    }

    override fun toString(): String {
        return "ClosedIntRanges" + myRanges.joinToString(separator = ",", prefix = "(", postfix = ")")
    }

    companion object {
        private val INTERSECTS_CMP: Comparator<ClosedIntRange> = Comparator { a, b ->
            return@Comparator if (a.intersects(b)) 0 else a.start - b.start
        }

        private fun toInsertPosition(x: Int): Int {
            return if (x < 0) -x - 1 else x
        }
    }
}