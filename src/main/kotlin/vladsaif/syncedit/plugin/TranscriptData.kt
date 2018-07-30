package vladsaif.syncedit.plugin

import java.io.InputStream
import java.io.StringReader
import javax.xml.bind.JAXB
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "transcript")
class TranscriptData(
        @field:[XmlElement(name = "word") XmlElementWrapper(name = "words")]
        val words: List<WordData>
) {
    val text
        get() = words.map(WordData::text).joinToString(separator = " ")


    // JAXB needs to access default constructor via reflection and add elements
    // so we may abuse fact that ArrayList can be assigned to kotlin List
    @Suppress("unused")
    private constructor() : this(ArrayList())

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
    val data = listOf(WordData("a", ClosedIntRange(10, 20), false),
            WordData("a", ClosedIntRange(100, 200), false)).let {
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
        <word visible="false">
            <text>a</text>
            <range start="10" end="20"/>
        </word>
        <word visible="false">
            <text>a</text>
            <range start="100" end="200"/>
        </word>
    </words>
</transcript>""")) as TranscriptData
    println()
    println(obj.words)
    println(obj.words::class.java)
}