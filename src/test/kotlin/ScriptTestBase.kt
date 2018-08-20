package vladsaif.syncedit.plugin

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast

open class ScriptTestBase : LightCodeInsightFixtureTestCase() {

  protected fun createKtFile(text: String): KtFile {
    return createLightFile("file.kts", KotlinLanguage.INSTANCE, text).cast()
  }
}