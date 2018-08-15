package vladsaif.syncedit.plugin

import org.junit.Test
import kotlin.test.assertEquals

class TranscriptDataTest {

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
    val wordsData = words.zip(ranges).map { WordData(it.first, it.second, WordData.State.PRESENTED, -1) }
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
}