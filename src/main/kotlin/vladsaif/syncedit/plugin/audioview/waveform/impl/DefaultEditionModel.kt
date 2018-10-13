package vladsaif.syncedit.plugin.audioview.waveform.impl

import vladsaif.syncedit.plugin.LRange
import vladsaif.syncedit.plugin.LRangeUnion
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*


class DefaultEditionModel : ChangeNotifier by DefaultChangeNotifier(), EditionModel {
  private val myCutRanges = LRangeUnion()
  private val myMuteRanges = LRangeUnion()
  private val myNoChangesRanges = LRangeUnion()
  override val editions: List<Pair<LRange, EditionModel.EditionType>>
    get() {
      val ret: MutableList<Pair<LRange, EditionModel.EditionType>> = mutableListOf()
      ret.addAll(myCutRanges.ranges.map { it to CUT })
      ret.addAll(myMuteRanges.ranges.map { it to MUTE })
      ret.addAll(myNoChangesRanges.ranges.map { it to NO_CHANGES })
      ret.sortBy(Pair<LRange, EditionModel.EditionType>::first)
      return ret.toList()
    }

  init {
    // 256 is a maximum frame size in bytes
    myNoChangesRanges.union(LRange(0, Long.MAX_VALUE / 256))
  }

  override fun cut(frameRange: LRange) {
    myCutRanges.union(frameRange)
    myMuteRanges.exclude(frameRange)
    myNoChangesRanges.exclude(frameRange)
    fireStateChanged()
  }

  override fun mute(frameRange: LRange) {
    myCutRanges.exclude(frameRange)
    myMuteRanges.union(frameRange)
    myNoChangesRanges.exclude(frameRange)
    fireStateChanged()
  }

  override fun undo(frameRange: LRange) {
    myCutRanges.exclude(frameRange)
    myMuteRanges.exclude(frameRange)
    myNoChangesRanges.union(frameRange)
    fireStateChanged()
  }

  override fun reset() {
    val infinity = LRange(0, Long.MAX_VALUE / 256)
    myCutRanges.exclude(infinity)
    myMuteRanges.exclude(infinity)
    myNoChangesRanges.union(infinity)
    fireStateChanged()
  }

  override fun copy(): EditionModel {
    val copy = DefaultEditionModel()
    for (range in myCutRanges.ranges) {
      copy.cut(range)
    }
    for (range in myMuteRanges.ranges) {
      copy.mute(range)
    }
    return copy
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DefaultEditionModel

    if (myCutRanges != other.myCutRanges) return false
    if (myMuteRanges != other.myMuteRanges) return false
    if (myNoChangesRanges != other.myNoChangesRanges) return false

    return true
  }

  override fun hashCode(): Int {
    var result = myCutRanges.hashCode()
    result = 31 * result + myMuteRanges.hashCode()
    result = 31 * result + myNoChangesRanges.hashCode()
    return result
  }
}
