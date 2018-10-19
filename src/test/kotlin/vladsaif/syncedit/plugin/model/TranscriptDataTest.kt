package vladsaif.syncedit.plugin.model

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test
import vladsaif.syncedit.plugin.util.*

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
    val ranges = (1..words.size).map { IRange(it, it + 1) }
    val wordsData = words.zip(ranges).map { WordData(it.first, it.second) }
    return TranscriptData(wordsData)
  }

  @Test
  fun `test replacement`() {
    val newWord = myTestData.words[2].copy(text = "instead third")
    val newData = myTestData.replaceWords(listOf(2 to newWord))
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
      assertEquals(myTestData, TranscriptData(myTestData.words.shuffled()))
    }
  }

  @Test
  fun `test rename word`() {
    val newData = myTestData.renameWord(2, "new text")
    assertEquals("new text", newData[2].filteredText)
  }

  @Test
  fun `test nbsp filter`() {
    val newData = myTestData.renameWord(0, "\u00A0\u00A0")
    assertEquals("  ", newData[0].filteredText)
  }

  @Test
  fun `test bindings`() {
    val factory = createMarkerFactory(20)
    val bindData = mapOf(
        1 to factory(IRange(1, 3)),
        2 to factory(IRange(1, 3)),
        3 to factory(IRange(1, 4)),
        5 to factory(IRange(6, 9)),
        6 to factory(IRange(6, 9)),
        7 to factory(IRange(6, 9))
    )
    val expectedBindings = listOf(
        MergedLineMapping(IRange(1, 2), IRange(1, 3)),
        MergedLineMapping(IRange(3, 3), IRange(1, 4)),
        MergedLineMapping(IRange(5, 7), IRange(6, 9))
    )
    assertEquals(expectedBindings, createMergedLineMappings(bindData.mapValues { (_, v) -> v.toLineRange() }))
  }

  @Test
  fun `test merge bindings`() {
    val bindings = listOf(
        MergedLineMapping(IRange(1, 3), IRange(5, 6)),
        MergedLineMapping(IRange(2, 4), IRange(5, 6)),
        MergedLineMapping(IRange(2, 7), IRange(5, 6)),
        MergedLineMapping(IRange(4, 7), IRange(5, 7)),
        MergedLineMapping(IRange(9, 10), IRange(5, 7)),
        MergedLineMapping(IRange(11, 12), IRange(5, 7)),
        MergedLineMapping(IRange(14, 15), IRange(10, 11)),
        MergedLineMapping(IRange(17, 18), IRange(10, 11))
    )
    val merged = mergeLineMappings(bindings)
    val expected = listOf(
        MergedLineMapping(IRange(1, 7), IRange(5, 6)),
        MergedLineMapping(IRange(4, 7), IRange(5, 7)),
        MergedLineMapping(IRange(9, 12), IRange(5, 7)),
        MergedLineMapping(IRange(14, 15), IRange(10, 11)),
        MergedLineMapping(IRange(17, 18), IRange(10, 11))
    )
    assertEquals(expected, merged)
  }

  private fun createMarkerFactory(lines: Int): (IRange) -> RangeMarker {
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