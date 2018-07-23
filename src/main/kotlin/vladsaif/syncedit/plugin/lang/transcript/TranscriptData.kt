package vladsaif.syncedit.plugin.lang.transcript

import java.io.InputStream
import java.io.StringReader
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.annotation.*

@XmlRootElement(name = "transcript")
data class TranscriptData(
        @field:[XmlElement(name = "word") XmlElementWrapper(name = "words")]
        val words: List<WordData>) {
    val text
        get() = words.map(WordData::text).joinToString(separator = " ")

    // JAXB needs to access default constructor via reflection and add elements
    // so we may abuse fact that ArrayList can be assigned to kotlin List
    @Suppress("unused")
    private constructor() : this(ArrayList())

    @XmlAccessorType(XmlAccessType.FIELD)
    data class WordData(
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
        @Suppress("unused")
        private constructor() : this("", 0, 0, false)
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

fun main(args: Array<String>) {
    val data = listOf(TranscriptData.WordData("a", 0, 0, false), TranscriptData.WordData("a", 0, 0, false)).let {
        TranscriptData(it)
    }
    val context = JAXBContext.newInstance(TranscriptData::class.java)
    val marshaller = context.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    marshaller.marshal(data, System.out)
    val unmarshaler = context.createUnmarshaller()
    val obj = unmarshaler.unmarshal(StringReader("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<transcript>
    <words>
        <word start="0" end="0" visible="false">a</word>
        <word start="0" end="0" visible="false">a</word>
    </words>
</transcript>""")) as TranscriptData
    println()
    println(obj.words)
    println(obj.words::class.java)
}