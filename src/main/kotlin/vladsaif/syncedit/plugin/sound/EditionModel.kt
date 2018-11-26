package vladsaif.syncedit.plugin.sound

import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.util.end
import java.io.StringReader
import java.util.*

interface EditionModelView {

  /**
   * @return Sorted by first element of pair list of editions that were made.
   */
  val editions: List<Pair<LongRange, EditionModel.EditionType>>

  fun impose(frameRange: LongRange): LongRange

  fun impose(frame: Long): Long {
    return impose(frame..frame).start
  }

  fun overlay(frameRange: LongRange): LongRange

  fun overlay(frame: Long): Long {
    return overlay(frame..frame).start
  }

  fun copy(): EditionModel

  fun serialize(): ByteArray {
    return buildString {
      for ((range, type) in editions) {
        append("${range.start} ${range.end} $type\n")
      }
    }.toByteArray(Charsets.UTF_8)
  }
}

interface EditionModel : EditionModelView {
  enum class EditionType {
    CUT, MUTE, NO_CHANGES
  }

  /**
   * Cut a frame range.
   *
   * If [frameRange] intersects ranges that were muted, they become cut and not muted.
   */
  fun cut(frameRange: LongRange)

  /**
   * Mute a frame range.
   *
   * If [frameRange] intersects ranges that were cut, they become muted and not cut.
   */
  fun mute(frameRange: LongRange)

  /**
   * Undo all changes made to this [frameRange]
   */
  fun unmute(frameRange: LongRange)

  /**
   * Reset all changes.
   */
  fun reset()

  fun shift(delta: Long)

  fun load(other: EditionModel) {
    reset()
    for ((range, type) in other.editions) {
      when (type) {
        CUT -> cut(range)
        MUTE -> mute(range)
        NO_CHANGES -> Unit
      }
    }
  }

  companion object {

    fun deserialize(bytes: ByteArray): EditionModel {
      with(DefaultEditionModel()) {
        val sc = Scanner(StringReader(String(bytes, Charsets.UTF_8)))
        while (sc.hasNext()) {
          val range = LongRange(sc.nextLong(), sc.nextLong())
          when (sc.next()) {
            CUT.name -> cut(range)
            MUTE.name -> mute(range)
            NO_CHANGES.name -> unmute(range)
          }
        }
        return this
      }
    }
  }
}