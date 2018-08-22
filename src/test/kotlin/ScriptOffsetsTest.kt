package vladsaif.syncedit.plugin

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.junit.Test
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.script.psi.TimedLines
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptOffsetsTest : LightCodeInsightFixtureTestCase() {

  private fun createKtFile(text: String): KtFile {
    return createLightFile("file.kts", KotlinLanguage.INSTANCE, text).cast()
  }

  private fun extractPsiElement(text: String): PsiElement? {
    val file = createKtFile(text)
    return file.script!!.blockExpression.statements.firstOrNull()
  }

  @Test
  fun `test simple offset`() {
    assertTrue {
      TimeOffsetParser.isTimeOffset(extractPsiElement("timeOffset(10L)")!!)
    }
  }

  @Test
  fun `test offset with named argument`() {
    assertTrue {
      TimeOffsetParser.isTimeOffset(extractPsiElement("timeOffset(ms = 10L)")!!)
    }
  }

  @Test
  fun `test not offset int arg`() {
    assertFalse {
      TimeOffsetParser.isTimeOffset(extractPsiElement("timeOffset(10)")!!)
    }
  }

  @Test
  fun `test other fun`() {
    assertFalse {
      TimeOffsetParser.isTimeOffset(extractPsiElement("listOf(10)")!!)
    }
  }

  @Test
  fun `test commented time offset`() {
    assertNull(extractPsiElement("//timeOffset(10L)"))
  }

  @Test
  fun `test offset value`() {
    assertEquals(1000, TimeOffsetParser.parseOffset("timeOffset(1000L)"))
  }

  @Test
  fun `test offset value when named argument`() {
    assertEquals(1000, TimeOffsetParser.parseOffset("timeOffset(ms =  1000L)"))
  }

  @Test
  fun `test correct detecting statements`() {
    val text = """
      timeOffset(ms = 2000L)
      statement()
      call()
      anotherCall()

      timeOffset(3000L)
      startBlock {
        call()
        timeOffset(4000L)
      }
      timeOffset(5000L)
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = listOf(
        TimedLines(lines = IRange(1, 1), time = IRange(2000, 3000)), // statement()
        TimedLines(lines = IRange(2, 2), time = IRange(2000, 3000)), // call()
        TimedLines(lines = IRange(3, 3), time = IRange(2000, 3000)), // anotherCall()
        TimedLines(lines = IRange(6, 9), time = IRange(3000, 5000)), // block
        TimedLines(lines = IRange(7, 7), time = IRange(3000, 4000)) // call()
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `test no first offset`() {
    val text = """
      statement()
      call()
      anotherCall()
      timeOffset(3000L)
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = listOf(
        TimedLines(lines = IRange(0, 0), time = IRange(0, 3000)), // statement()
        TimedLines(lines = IRange(1, 1), time = IRange(0, 3000)), // call()
        TimedLines(lines = IRange(2, 2), time = IRange(0, 3000))  // anotherCall()
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `test no end time offset`() {
    val text = """
      timeOffset(3000L)
      startBlock {
        timeOffset(4000L)
      }
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    assertEquals(listOf(TimedLines(IRange(1, 3), IRange.EMPTY_RANGE)), actual)
  }

  @Test
  fun `test no offsets`() {
    val text = """
      startBlock {
      }
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    assertEquals(listOf(TimedLines(IRange(0, 1), IRange.EMPTY_RANGE)), actual)
  }
}