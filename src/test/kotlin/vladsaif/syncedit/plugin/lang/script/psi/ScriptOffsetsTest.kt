package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test
import vladsaif.syncedit.plugin.createKtFile
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScriptOffsetsTest : LightCodeInsightFixtureTestCase() {

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

      timeOffset(1000L)
      startBlock {
        call()
        timeOffset(1000L)
      }
      timeOffset(1000L)
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = codeBlockModel {
      statement("statement()", 2000..3000)
      statement("call()", 2000..3000)
      statement("anotherCall()", 2000..3000)
      block("startBlock", 3000..5000) {
        statement("call()", 3000..4000)
      }
    }
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
    val expected = codeBlockModel {
      statement("statement()", 0..3000)
      statement("call()", 0..3000)
      statement("anotherCall()", 0..3000)
    }
    assertEquals(expected, actual)
  }

  @Test
  fun `test no end time offset`() {
    val text = """
      timeOffset(3000L)
      startBlock {
        timeOffset(1000L)
      }
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = codeBlockModel {
    }
    assertEquals(expected, actual)
  }

  @Test
  fun `test empty block`() {
    val text = """
      timeOffset(3000L)
      startBlock {
        timeOffset(1000L)
      }
      timeOffset(1000L)
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = codeBlockModel {
      block("startBlock", 3000..5000) {
      }
    }
    assertEquals(expected, actual)
  }

  @Test
  fun `test no offsets`() {
    val text = """
      startBlock {
      }
    """.trimIndent()
    val ktFile = createKtFile(text)
    val actual = TimeOffsetParser.parse(ktFile)
    val expected = codeBlockModel {
    }
    assertEquals(expected, actual)
  }
}