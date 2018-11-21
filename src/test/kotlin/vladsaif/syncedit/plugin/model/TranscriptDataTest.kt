package vladsaif.syncedit.plugin.model

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test
import vladsaif.syncedit.plugin.util.*
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class TranscriptDataTest : LightCodeInsightFixtureTestCase() {

  private val myTestData = createTestData()

  private fun createTestData(): TranscriptData {
    val words = listOf(
      "first",
      "second",
      "third",
      "fourth",
      "fifth",
      "sixth"
    )
    val ranges = (1..words.size).map { IntRange(it, it + 1) }
    val wordsData = words.zip(ranges).map { WordData(it.first, it.second) }
    return TranscriptData(wordsData, listOf())
  }

  @Test
  fun `test replacement`() {
    val newWord = myTestData.words[2].copy(text = "instead third")
    val newData = myTestData.replace(listOf(2 to newWord))
    for ((i, word) in newData.words.withIndex()) {
      if (i == 2) {
        assertEquals(newWord, word)
      } else {
        assertEquals(word, myTestData.words[i])
      }
    }
  }

  @Test
  fun `test sorting words`() {
    for (i in 1..10) {
      assertEquals(myTestData, TranscriptData(myTestData.words.shuffled(), listOf()))
    }
  }

  @Test
  fun `test rename word`() {
    val newData = myTestData.rename(2, "new text")
    assertEquals("new text", newData[2].filteredText)
  }

  @Test
  fun `test delete word`() {
    val newData = myTestData.delete(2)
    assertEquals("third", newData.deletedWords[0].filteredText)
  }

  @Test
  fun `test nbsp filter`() {
    val newData = myTestData.rename(0, "\u00A0\u00A0")
    assertEquals("  ", newData[0].filteredText)
  }

  @Test
  fun `test intersecting ranges`() {
    val data = TranscriptData(listOf(
      "one" to 100..200,
      "two" to 150..250,
      "three" to 230..500,
      "four" to 300..400
    ).map { (a, b) -> WordData(a, b) })
    assertEquals(
      listOf("one", "two", "three", "four"),
      data.words.map { it.filteredText }
    )
    assertTrue(data.words.zipWithNext().all { (a, b) -> !a.range.intersects(b.range) })
  }

  @Test
  fun `test jaxb serialization`() {
    val data = TranscriptData(
      listOf(
        WordData("first", IntRange(10, 20), WordData.State.PRESENTED),
        WordData("second", IntRange(100, 200), WordData.State.PRESENTED)
      )
    )
    val context = JAXBContext.newInstance(TranscriptData::class.java)
    val marshaller = context.createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    marshaller.marshal(data, System.out)
  }

  @Test
  fun `test jaxb deserialization`() {
    val context = JAXBContext.newInstance(TranscriptData::class.java)
    val unmarshaler = context.createUnmarshaller()
    val obj = unmarshaler.unmarshal(
      StringReader(
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
</transcript>"""
      )
    ) as TranscriptData
    val data = TranscriptData(
      listOf(
        WordData("first", IntRange(10, 20), WordData.State.PRESENTED),
        WordData("second", IntRange(100, 200), WordData.State.PRESENTED)
      )
    )
    kotlin.test.assertEquals(data, obj)
  }

  @Test
  fun `test bindings`() {
    val factory = createMarkerFactory(20)
    val bindData = mapOf(
      1 to factory(IntRange(1, 3)),
      2 to factory(IntRange(1, 3)),
      3 to factory(IntRange(1, 4)),
      5 to factory(IntRange(6, 9)),
      6 to factory(IntRange(6, 9)),
      7 to factory(IntRange(6, 9))
    )
    val expectedBindings = listOf(
      MergedLineMapping(IntRange(1, 2), IntRange(1, 3)),
      MergedLineMapping(IntRange(3, 3), IntRange(1, 4)),
      MergedLineMapping(IntRange(5, 7), IntRange(6, 9))
    )
    assertEquals(expectedBindings, createMergedLineMappings(bindData.mapValues { (_, v) -> v.toLineRange() }))
  }

  @Test
  fun `test merge bindings`() {
    val bindings = listOf(
      MergedLineMapping(IntRange(1, 3), IntRange(5, 6)),
      MergedLineMapping(IntRange(2, 4), IntRange(5, 6)),
      MergedLineMapping(IntRange(2, 7), IntRange(5, 6)),
      MergedLineMapping(IntRange(4, 7), IntRange(5, 7)),
      MergedLineMapping(IntRange(9, 10), IntRange(5, 7)),
      MergedLineMapping(IntRange(11, 12), IntRange(5, 7)),
      MergedLineMapping(IntRange(14, 15), IntRange(10, 11)),
      MergedLineMapping(IntRange(17, 18), IntRange(10, 11))
    )
    val merged = mergeLineMappings(bindings)
    val expected = listOf(
      MergedLineMapping(IntRange(1, 7), IntRange(5, 6)),
      MergedLineMapping(IntRange(4, 7), IntRange(5, 7)),
      MergedLineMapping(IntRange(9, 12), IntRange(5, 7)),
      MergedLineMapping(IntRange(14, 15), IntRange(10, 11)),
      MergedLineMapping(IntRange(17, 18), IntRange(10, 11))
    )
    assertEquals(expected, merged)
  }

  private fun createMarkerFactory(lines: Int): (IntRange) -> RangeMarker {
    val doc = createDocument(lines)
    return {
      doc.createRangeMarker(doc.getLineStartOffset(it.start), doc.getLineEndOffset(it.end))
    }
  }

  private fun createDocument(lines: Int): Document {
    return createLightFile(XmlFileType.INSTANCE, (1..lines).map { "_" }.joinToString(separator = "\n") { it })
      .viewProvider
      .document!!
  }
}