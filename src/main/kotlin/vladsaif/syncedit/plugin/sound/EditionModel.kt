package vladsaif.syncedit.plugin.sound

import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.util.LRange
import java.io.StringReader
import java.util.*

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

  fun copy(): EditionModel

  fun serialize(): ByteArray {
    return buildString {
      for ((range, type) in editions) {
        append("${range.start} ${range.end} $type\n")
      }
    }.toByteArray(Charsets.UTF_8)
  }

  companion object {

    fun deserialize(bytes: ByteArray): EditionModel {
      with(DefaultEditionModel()) {
        val sc = Scanner(StringReader(String(bytes, Charsets.UTF_8)))
        while (sc.hasNext()) {
          val range = LRange(sc.nextLong(), sc.nextLong())
          when (sc.next()) {
            CUT.name -> cut(range)
            MUTE.name -> mute(range)
            NO_CHANGES.name -> undo(range)
          }
        }
        return this
      }
    }
  }
}