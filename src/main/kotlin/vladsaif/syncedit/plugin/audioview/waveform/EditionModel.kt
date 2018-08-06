package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.ClosedLongRange

interface EditionModel : ChangeNotifier {
  enum class EditionType {
    CUT, MUTE, NO_CHANGES
  }

  /**
   * @return Sorted by first element of pair list of editions that were made.
   */
  val editions: List<Pair<ClosedLongRange, EditionType>>

  /**
   * Cut a frame range.
   *
   * If [frameRange] intersects ranges that were muted, they become cut and not muted.
   */
  fun cut(frameRange: ClosedLongRange)

  /**
   * Mute a frame range.
   *
   * If [frameRange] intersects ranges that were cut, they become muted and not cut.
   */
  fun mute(frameRange: ClosedLongRange)

  /**
   * Undo all changes made to this [frameRange]
   */
  fun undo(frameRange: ClosedLongRange)

}