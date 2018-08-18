package vladsaif.syncedit.plugin

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class WordData(
    @field:XmlElement
    private val text: String,
    @field:XmlElement
    val range: IRange,
    @field:XmlAttribute
    val state: State = State.PRESENTED,
    @field:XmlElement(name = "bind")
    val bindStatements: IRange = IRange.EMPTY_RANGE
) : Comparable<WordData> {
  /**
   * Replaces non-breaking space symbol because it is used in TranscriptView language as word separator
   */
  val filteredText: String
    get() = if (text.contains('\u00A0')) text.replace('\u00A0', ' ') else text
  val isBind: Boolean
    get() = !bindStatements.empty

  // JAXB constructor
  @Suppress("unused")
  private constructor() : this("", IRange.EMPTY_RANGE)

  enum class State {
    EXCLUDED, MUTED, PRESENTED
  }

  override fun compareTo(other: WordData) = range.compareTo(other.range)
}