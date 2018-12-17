package com.github.recognized.screencast.editor.model

import com.github.recognized.kotlin.ranges.extensions.intersects
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test

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
    return TranscriptData.create(wordsData, listOf())
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
      assertEquals(myTestData, TranscriptData.create(myTestData.words.shuffled(), listOf()))
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
    val data = TranscriptData.create(listOf(
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
  fun `test serialization`() {
    val data = TranscriptData.create(
      listOf(
        WordData("first", IntRange(10, 20), WordData.State.PRESENTED),
        WordData("second", IntRange(100, 200), WordData.State.PRESENTED)
      )
    )
    assertEquals(data, TranscriptData.createFrom(data.toXml()))
  }
}