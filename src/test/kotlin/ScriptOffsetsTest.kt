package vladsaif.syncedit.plugin

import org.junit.Test
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptOffsetsTest {

  @Test
  fun `test simple offset`() {
    assertTrue {
      TimeOffsetParser.isTimeOffset("timeOffset(10L)")
    }
  }

  @Test
  fun `test offset with named argument`() {
    assertTrue {
      TimeOffsetParser.isTimeOffset("timeOffset(ms = 10L)")
    }
  }

  @Test
  fun `test not offset int arg`() {
    assertFalse {
      TimeOffsetParser.isTimeOffset("timeOffset(10)")
    }
  }

  @Test
  fun `test other fun`() {
    assertFalse {
      TimeOffsetParser.isTimeOffset("listOf(10)")
    }
  }

  @Test
  fun `test commented time offset`() {
    assertFalse {
      TimeOffsetParser.isTimeOffset("//timeOffset(10L)")
    }
  }

  @Test
  fun `test offset value`() {
    assertEquals(1000, TimeOffsetParser.parseOffset("timeOffset(1000L)"))
  }

  @Test
  fun `test offset value when named argument`() {
    assertEquals(1000, TimeOffsetParser.parseOffset("timeOffset(ms =  1000L)"))
  }

  private val myText = """
      timeOffset(ms = 2000L)
      statement()
      call()
      anotherCall()

      timeOffset(3000L)
      startBlock {
        call()
        timeOffset(4000L)
      }
    """.trimIndent()

  @Test
  fun `test correct detecting statements`() {
    val linesWithOffsets = listOf(0, 5, 8)
    val actual = TimeOffsetParser.getOffsetToLineList(myText.split('\n'))
        .map { it.second }
        .sorted()
    assertEquals(linesWithOffsets, actual)
  }

  @Test
  fun `test time mapping`() {
    val result = TimeOffsetParser.parseText(myText)
    val expected = mapOf(
        IRange(2000, 3000) to IRange(1, 4),
        IRange(3000, 4000) to IRange(6, 7)
    )
    assertEquals(expected, result)
  }
}