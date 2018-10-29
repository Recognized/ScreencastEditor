package vladsaif.syncedit.plugin.model

import vladsaif.syncedit.plugin.util.compareTo
import vladsaif.syncedit.plugin.util.end
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlAccessorType(XmlAccessType.FIELD)
data class WordData(
  @field:XmlElement
  private val text: String,
  @field:XmlJavaTypeAdapter(IntRangeAdapter::class)
  val range: IntRange,
  @field:XmlAttribute
  val state: State = State.PRESENTED
) : Comparable<WordData> {
  /**
   * Replaces non-breaking space symbol because it is used in TranscriptView language as word separator
   */
  val filteredText: String
    get() = if (text.contains('\u00A0')) text.replace('\u00A0', ' ') else text

  // JAXB constructor
  @Suppress("unused")
  private constructor() : this("", IntRange.EMPTY)

  enum class State {
    EXCLUDED, MUTED, PRESENTED
  }

  override fun compareTo(other: WordData) = range.compareTo(other.range)
}

@XmlAccessorType(XmlAccessType.FIELD)
private class IntRangeAdapter(
  @field:XmlAttribute
  var start: Int = 0,
  @field:XmlAttribute
  var end: Int = 0
) : XmlAdapter<IntRangeAdapter, IntRange>() {

  override fun marshal(v: IntRange?): IntRangeAdapter {
    return IntRangeAdapter(v?.start ?: 0, v?.end ?: -1)
  }

  override fun unmarshal(v: IntRangeAdapter?): IntRange {
    return v?.let { it.start..it.end } ?: IntRange.EMPTY
  }
}