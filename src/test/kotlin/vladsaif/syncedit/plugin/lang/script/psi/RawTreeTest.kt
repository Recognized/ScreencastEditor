package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.junit.Test
import vladsaif.syncedit.plugin.createKtFile

class RawTreeTest : LightCodeInsightFixtureTestCase() {

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
      block("ideFrame", 1000..10000) {
        statement("action", 1000)
        block("editor", 2000..3000) {
          statement("typeText", 2500)
        }
        block("toolsMenu", 4000..9000) {
          block("chooseFile", 5000..7000) {
            statement("buttonClick", 6000)
          }
        }
      }
    }
    val text = """
      |hello
      |ideFrame {
      |  action
      |  editor {
      |    typeText
      |  }
      |  toolsMenu {
      |    chooseFile {
      |      buttonClick
      |    }
      |  }
      |}
    """.trimIndent().trimMargin()
    val expected = CodeModel(listOf(Statement("hello", 0)) + model.blocks)
    model.assertTransformed(expected, text)
  }

  private fun CodeModel.assertTransformed(expected: CodeModel, text: String) {
    assertEquals(expected, this.transformedByScript(createKtFile(text)))
  }
}