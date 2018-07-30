package vladsaif.syncedit.plugin

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class WordData(
        @field:XmlElement private val text: String,
        @field:XmlElement val range: ClosedIntRange,
        @field:XmlAttribute val visible: Boolean
) : Comparable<WordData> {

    // JAXB constructor
    @Suppress("unused")
    private constructor() : this("", ClosedIntRange.EMPTY_RANGE, false)

    override fun compareTo(other: WordData) = range.compareTo(other.range)

    override fun toString() = if (text.contains('\u00A0')) text.replace('\u00A0', ' ') else text
}