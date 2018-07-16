package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.ClosedLongRangeUnion
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*


class DefaultEditionModel : ChangeNotifier by DefaultChangeNotifier(), EditionModel {
    private val cut = ClosedLongRangeUnion()
    private val mute = ClosedLongRangeUnion()
    private val noChanges = ClosedLongRangeUnion()
    override val editions: List<Pair<ClosedLongRange, EditionModel.EditionType>>
        get() {
            val ret: MutableList<Pair<ClosedLongRange, EditionModel.EditionType>> = mutableListOf()
            ret.addAll(cut.ranges.map { it to CUT })
            ret.addAll(mute.ranges.map { it to MUTE })
            ret.addAll(noChanges.ranges.map { it to NO_CHANGES })
            ret.sortBy(Pair<ClosedLongRange, EditionModel.EditionType>::first)
            return ret.toList()
        }

    init {
        // 256 is a maximum frame size in bytes
        noChanges.union(ClosedLongRange(0, Long.MAX_VALUE / 256))
    }

    override fun cut(frameRange: ClosedLongRange) {
        cut.union(frameRange)
        mute.exclude(frameRange)
        noChanges.exclude(frameRange)
        fireStateChanged()
    }

    override fun mute(frameRange: ClosedLongRange) {
        cut.exclude(frameRange)
        mute.union(frameRange)
        noChanges.exclude(frameRange)
        fireStateChanged()
    }

    override fun undo(frameRange: ClosedLongRange) {
        cut.exclude(frameRange)
        mute.exclude(frameRange)
        noChanges.union(frameRange)
        fireStateChanged()
    }
}
