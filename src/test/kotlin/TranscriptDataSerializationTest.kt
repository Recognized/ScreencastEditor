package vladsaif.syncedit.plugin

import org.junit.Test
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import kotlin.test.assertEquals


class TranscriptDataSerializationTest {

  @Test
  fun `test jaxb serialization`() {
    val data = listOf(WordData("first", ClosedIntRange(10, 20), WordData.State.PRESENTED),
        WordData("second", ClosedIntRange(100, 200), WordData.State.PRESENTED)).let {
      TranscriptData(it)
    }
    val context = JAXBContext.newInstance(TranscriptData::class.java)
    val marshaller = context.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    marshaller.marshal(data, System.out)
  }

  @Test
  fun `test jaxb deserialization`() {
    val context = JAXBContext.newInstance(TranscriptData::class.java)
    val unmarshaler = context.createUnmarshaller()
    val obj = unmarshaler.unmarshal(StringReader("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<transcript>
    <words>
        <word state="PRESENTED">
            <text>first</text>
            <range start="10" end="20"/>
        </word>
        <word state="PRESENTED">
            <text>second</text>
            <range start="100" end="200"/>
        </word>
    </words>
</transcript>""")) as TranscriptData
    val data = listOf(
        WordData("first", ClosedIntRange(10, 20), WordData.State.PRESENTED),
        WordData("second", ClosedIntRange(100, 200), WordData.State.PRESENTED)
    ).let {
      TranscriptData(it)
    }
    assertEquals(data, obj)
  }
}