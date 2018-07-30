package vladsaif.syncedit.plugin

import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "transcript")
data class TranscriptData(
        @field:[XmlElement(name = "word") XmlElementWrapper(name = "words")]
        val words: List<WordData>
) {
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

    companion object {
        fun createFrom(xml: CharSequence): TranscriptData {
            return JAXB.unmarshal(StringReader(xml.toString()), TranscriptData::class.java)
        }

        fun createFrom(xml: InputStream): TranscriptData {
            return JAXB.unmarshal(xml, TranscriptData::class.java)
        }
    }
}