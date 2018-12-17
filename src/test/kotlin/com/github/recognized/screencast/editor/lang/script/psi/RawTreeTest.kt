package com.github.recognized.screencast.editor.lang.script.psi

import com.github.recognized.screencast.editor.createKtFile
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test

class RawTreeTest : LightCodeInsightFixtureTestCase() {

  private fun CodeBlockBuilder.createSomething() {
    block("ideFrame", 100..1000) {
      statement("statement1()", 200)
      statement("statement2()", 300)
      block("block3", 400..600) {
        statement("statement4", 500)
      }
    }
  }
  
  @Test
  fun `test plain tree building`() {
    val model = codeModel {
      block("ideFrame", 100..1000) {
        statement("statement1()", 200)
        statement("statement2()", 300)
        block("block3", 400..600) {
          statement("statement4", 500)
        }
      }
    }
    val result = """
      0: Offset(100)
      1..12: Code('ideFrame')
      2: Offset(200)
      3: Code('statement1()')
      4: Offset(300)
      5: Code('statement2()')
      6: Offset(400)
      7..10: Code('block3')
      8: Offset(500)
      9: Code('statement4')
      11: Offset(600)
      13: Offset(1000)
    """.trimIndent()
    assertEquals(result, RawTreeNode.buildPlainTree(model.createEditableTree()).joinToString("\n"))
  }

  @Test
  fun `test tree insertion transformation`() {
    val text = """
      ideFrame {
        statement1()
        statement2()
        statement3()
      }
    """.trimIndent()
    val codeModelBefore = codeModel {
      block("ideFrame", 100..1000) {
        statement("statement2()", 300)
      }
    }
    val expectedModel = codeModel {
      block("ideFrame", 100..1000) {
        statement("statement1()", 100)
        statement("statement2()", 300)
        statement("statement3()", 650)
      }
    }
    codeModelBefore.assertTransformed(expectedModel, text)
  }

  @Test
  fun `test tree swap lines transformation`() {
    val text = """
      ideFrame {
        AAA
        BBBBB
        CCCCCCC
      }
    """.trimIndent()
    val codeModelBefore = codeModel {
      block("ideFrame", 100..1000) {
        statement("AAA", 100)
        statement("CCCCCCC", 300)
        statement("BBBBB", 650)
      }
    }
    val expectedModel = codeModel {
      block("ideFrame", 100..1000) {
        statement("AAA", 100)
        statement("BBBBB", 650)
        statement("CCCCCCC", 825)
      }
    }
    codeModelBefore.assertTransformed(expectedModel, text)
  }

  @Test
  fun `test deletion`() {
    val text = """
      ideFrame {
        AAA
        CCCCCCC
      }
    """.trimIndent()
    val codeModelBefore = codeModel {
      block("ideFrame", 100..1000) {
        statement("AAA", 100)
        block("block", 200..400) {
          statement("hello", 300)
        }
        statement("CCCCCCC", 600)
      }
    }
    val expectedModel = codeModel {
      block("ideFrame", 100..1000) {
        statement("AAA", 100)
        statement("CCCCCCC", 600)
      }
    }
    codeModelBefore.assertTransformed(expectedModel, text)
  }

  @Test
  fun `test whole text shift`() {
    val model = codeModel {
      createSomething()
    }
    val expected = codeModel {
      statement("hello", 0)
      createSomething()
    }
    model.assertTransformed(expected)
  }

  fun `test paste block of code`() {
    val startModel = codeModel {
      block("bigBlock", 0..2000) {
        createSomething()
      }
    }
    val expectedModel = codeModel {
      block("bigBlock", 0..2000) {
        createSomething()
        block("anotherBlock", 1000..2000) {
          statement("click1()", 1333)
          statement("click2()", 1666)
        }
      }
    }
    startModel.assertTransformed(expectedModel)
  }

  @Test
  fun `test block expansion`() {
    val startModel = codeModel {
      block("frame", 0..1000) {
        statement("empty", 300)
      }
    }
    val expected = codeModel {
      block("frame", 0..1000) {
        block("empty", 300..1000) {
        }
      }
    }
    startModel.assertTransformed(expected)
  }

  private fun CodeModel.assertTransformed(
    expected: CodeModel,
    text: String = expected.createTextWithoutOffsets().text
  ) {
    assertEquals(expected, this.transformedByScript(createKtFile(text)))
  }
}