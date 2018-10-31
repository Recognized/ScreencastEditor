package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import vladsaif.syncedit.plugin.createKtFile

class RawTreeTest : LightCodeInsightFixtureTestCase() {

  fun `test tree transformation`() {
    val text = """
      ideFrame {
        statement1()
        statement2()
      }
    """.trimIndent()
    val codeModelBefore = codeModel {
      block("ideFrame", 100..1000) {
        statement("statement2()", 300)
      }
    }
    val expectedModel = codeModel {
      block("ideFrame", 100..1000) {
        statement("statement1()", 200)
        statement("statement2()", 300)
      }
    }
    codeModelBefore.assertTransformed(expectedModel, text)
  }

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
    println(RawTreeNode.buildPlainTree(model.createEditableTree()).joinToString("\n"))
  }

  private fun CodeModel.assertTransformed(expected: CodeModel, text: String) {
    assertEquals(expected, this.transformedByScript(createKtFile(text)))
  }
}