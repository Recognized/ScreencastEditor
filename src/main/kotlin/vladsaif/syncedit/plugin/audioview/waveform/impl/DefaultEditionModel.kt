package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.ClosedLongRangeUnion
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*


class DefaultEditionModel : ChangeNotifier by DefaultChangeNotifier(), EditionModel {
  private val myCutRanges = ClosedLongRangeUnion()
  private val myMuteRanges = ClosedLongRangeUnion()
  private val myNoChangesRanges = ClosedLongRangeUnion()
  override val editions: List<Pair<ClosedLongRange, EditionModel.EditionType>>
    get() {
      val ret: MutableList<Pair<ClosedLongRange, EditionModel.EditionType>> = mutableListOf()
      ret.addAll(myCutRanges.ranges.map { it to CUT })
      ret.addAll(myMuteRanges.ranges.map { it to MUTE })
      ret.addAll(myNoChangesRanges.ranges.map { it to NO_CHANGES })
      ret.sortBy(Pair<ClosedLongRange, EditionModel.EditionType>::first)
      return ret.toList()
    }

  init {
    // 256 is a maximum frame size in bytes
    myNoChangesRanges.union(ClosedLongRange(0, Long.MAX_VALUE / 256))
  }

  override fun cut(frameRange: ClosedLongRange) {
    myCutRanges.union(frameRange)
    myMuteRanges.exclude(frameRange)
    myNoChangesRanges.exclude(frameRange)
    fireStateChanged()
  }

  override fun mute(frameRange: ClosedLongRange) {
    myCutRanges.exclude(frameRange)
    myMuteRanges.union(frameRange)
    myNoChangesRanges.exclude(frameRange)
    fireStateChanged()
  }

  override fun undo(frameRange: ClosedLongRange) {
    myCutRanges.exclude(frameRange)
    myMuteRanges.exclude(frameRange)
    myNoChangesRanges.union(frameRange)
    fireStateChanged()
  }
}
