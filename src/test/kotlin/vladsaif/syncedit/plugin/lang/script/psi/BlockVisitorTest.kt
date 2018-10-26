package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import vladsaif.syncedit.plugin.createKtFile
import java.util.*

open class BlockVisitorTest : LightCodeInsightFixtureTestCase() {

  fun `test block structure`() {
    val code = """
      call()
      dot.qualified.call()
      val property = ""
      // Comment
      println(2 * 2 * 2)
      /**
       * Block comment
       */
      lambda() {
        nestedExpression()
        val nestedProperty = ""
      }
      fun decl() {}
    """.trimIndent()
    val script = createKtFile(code)
    val filteredActual = StringJoiner("\n")
    BlockVisitor.visit(script) { filteredActual.add(it.text) }
    val filteredExpected = """
      call()
      dot.qualified.call()
      println(2 * 2 * 2)
      lambda() {
        nestedExpression()
        val nestedProperty = ""
      }
      nestedExpression()
    """.trimIndent()
    assertEquals(
        filteredExpected.replace("\\s+".toRegex(), ""),
        filteredActual.toString().replace("\\s+".toRegex(), "")
    )
  }

}