package vladsaif.syncedit.plugin

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

val RESOURCES_PATH: Path = Paths.get("src", "test", "resources")

fun LightCodeInsightFixtureTestCase.createKtFile(text: String): KtFile {
  return createLightFile("file.kts", KotlinLanguage.INSTANCE, text).cast()
}

val CREDENTIALS_PATH: Path? = System.getenv("GOOGLE_CREDENTIALS")?.let { File(it).toPath() }
