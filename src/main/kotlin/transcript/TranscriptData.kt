package vladsaif.syncedit.plugin.transcript

import javax.xml.bind.JAXB
import javax.xml.bind.annotation.*

@XmlRootElement(name = "transcript")
class TranscriptData(@field:XmlElementWrapper(name = "words")
                     @field:XmlElement(name = "word")
                     val words: MutableList<WordData>) {
    // JAXB constructor
    @SuppressWarnings("unused")
    private constructor() : this(mutableListOf())

    @XmlAccessorType(XmlAccessType.FIELD)
    class WordData(
            @field:XmlValue
            val text: String,
            @field:XmlAttribute
            val start: Int,
            @field:XmlAttribute
            val end: Int,
            @field:XmlAttribute
            var visible: Boolean
    ) {
        // JAXB constructor
        @SuppressWarnings("unused")
        private constructor() : this("", 0, 0, false)
    }

    companion object {
        fun createFrom(xml: CharSequence): TranscriptData {
            return JAXB.unmarshal(xml.toString(), TranscriptData::class.java)
        }
    }
}