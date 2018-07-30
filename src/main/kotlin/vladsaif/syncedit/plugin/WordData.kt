package vladsaif.syncedit.plugin

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
class WordData(
        text: String,
        @field:XmlElement val range: ClosedIntRange,
        @field:XmlAttribute val visible: Boolean
) : Comparable<WordData> {
    // Use non-breaking space to prevent tokens from being separated by ordinary space
    @field:XmlElement
    val text = text.replace(' ', '\u00A0')

    // JAXB constructor
    @Suppress("unused")
    private constructor() : this("", ClosedIntRange.EMPTY_RANGE, false)

    override fun compareTo(other: WordData) = range.compareTo(other.range)

    override fun hashCode(): Int {
        var result = range.hashCode()
        result = 31 * result + visible.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WordData

        if (range != other.range) return false
        if (visible != other.visible) return false
        if (text != other.text) return false

        return true
    }

    fun copy(
            text: String = this.text,
            range: ClosedIntRange = this.range,
            visible: Boolean = this.visible
    ): WordData {
        return WordData(text, range, visible)
    }

    override fun toString(): String {
        return "WordData(range=$range, visible=$visible, text='$text')"
    }


}