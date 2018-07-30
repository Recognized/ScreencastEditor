package vladsaif.syncedit.plugin

import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "transcript")
class TranscriptData(words: List<WordData>) {
    @field:[XmlElement(name = "word") XmlElementWrapper(name = "words")]
    val words = words.sorted()
    val text
        get() = words.joinToString(separator = "\u00A0")

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


    companion object {
        fun createFrom(xml: CharSequence): TranscriptData {
            return JAXB.unmarshal(StringReader(xml.toString()), TranscriptData::class.java)
        }

        fun createFrom(xml: InputStream): TranscriptData {
            return JAXB.unmarshal(xml, TranscriptData::class.java)
        }
    }
}