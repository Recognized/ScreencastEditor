package vladsaif.syncedit.plugin.util

import java.util.concurrent.TimeUnit.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextFormatterTest {
  private val myText = "one two three, four five. Six seven eight, nine ten eleven"

  private fun List<String>.join(): String {
    return this.joinToString(separator = " ") { it }
  }

  @Test
  fun `test text preserves`() {
    val words = myText.split("\\s+".toRegex())
    val split = TextFormatter.splitText(myText, 10)
    for (word in words) {
      assertTrue("Not found: $word in '${words.join()}'") {
        split.any { it.contains(word) }
      }
    }
  }

  @Test
  fun `test very short line`() {
    val split = TextFormatter.splitText(myText, 0)
    val shortened = split.map { TextFormatter.createEllipsis(it, 0) }
    assertTrue("Text: '${shortened.join()}'") {
      shortened.all { it.isEmpty() }
    }
  }

  @Test
  fun `test short line`() {
    val text = "aaaaa bbbbb ccccc ddddd"
    val split = TextFormatter.splitText(text, 4)
    val shortened = split.map { TextFormatter.createEllipsis(it, 4) }
    assertTrue("Text: '${shortened.join()}'") {
      shortened.all { it.endsWith("...") && it.length == 4 }
    }
  }

  @Test
  fun `test long line`() {
    assertEquals(TextFormatter.splitText(myText, 1000).map {
      TextFormatter.createEllipsis(it, 1000)
    }.join(), myText)
  }

  @Test
  fun `test zero nanoseconds`() {
    assertEquals("0.0", TextFormatter.formatTime(0).dropLast(8))
  }

  @Test
  fun `test one minute`() {
    assertEquals(
      "1:00.0",
      TextFormatter.formatTime(NANOSECONDS.convert(1, MINUTES)).dropLast(8)
    )
  }

  @Test
  fun `test ten minutes, twenty seconds and 30ms`() {
    val ns = NANOSECONDS.convert(10, MINUTES) +
        NANOSECONDS.convert(20, SECONDS) +
        NANOSECONDS.convert(30, MILLISECONDS)
    assertEquals(
      "10:20.03",
      TextFormatter.formatTime(ns).dropLast(7)
    )
  }
}