package vladsaif.syncedit.plugin

import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlRootElement(name = "transcript")
class TranscriptData(words: List<WordData>) {
    @field:[XmlJavaTypeAdapter(WordsAdapter::class)]
    val words = words.sorted()
    val text
        get() = words.joinToString(separator = "\u00A0") { it.filteredText }

    // JAXB needs to access default constructor via reflection and add elements
    // so we may abuse fact that ArrayList can be assigned to kotlin List
    @Suppress("unused")
    private constructor() : this(ArrayList())

    fun toXml(): String {
        val writer = StringWriter()
        JAXB.marshal(this, writer)
        return writer.toString()
    }

    override fun toString() = "TranscriptData($words)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranscriptData

        if (words != other.words) return false

        return true
    }

    override fun hashCode(): Int {
        return words.hashCode()
    }

    private class WordsAdapter : XmlAdapter<WordsValueType, List<WordData>>() {
        override fun marshal(v: List<WordData>?): WordsValueType {
            return WordsValueType().apply { words = if (v != null) ArrayList(v) else ArrayList() }
        }

        override fun unmarshal(v: WordsValueType?): List<WordData> {
            return v?.words?.toList() ?: emptyList()
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    private class WordsValueType {
        @field:[XmlElement(name = "word")]
        lateinit var words: ArrayList<WordData>
    }

    companion object {
        fun createFrom(xml: CharSequence): TranscriptData {
            return JAXB.unmarshal(StringReader(xml.toString()), TranscriptData::class.java)
        }

        fun createFrom(xml: InputStream): TranscriptData {
            return JAXB.unmarshal(xml, TranscriptData::class.java)
        }
    }
}