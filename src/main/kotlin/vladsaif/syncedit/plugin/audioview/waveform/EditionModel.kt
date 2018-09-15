package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.LRange

interface EditionModel : ChangeNotifier {
  enum class EditionType {
    CUT, MUTE, NO_CHANGES
  }

  /**
   * @return Sorted by first element of pair list of editions that were made.
   */
  val editions: List<Pair<LRange, EditionType>>

  /**
   * Cut a frame range.
   *
   * If [frameRange] intersects ranges that were muted, they become cut and not muted.
   */
  fun cut(frameRange: LRange)

  /**
   * Mute a frame range.
   *
   * If [frameRange] intersects ranges that were cut, they become muted and not cut.
   */
  fun mute(frameRange: LRange)

  /**
   * Undo all changes made to this [frameRange]
   */
  fun undo(frameRange: LRange)


  /**
   * Reset all changes.
   */
  fun reset()

}