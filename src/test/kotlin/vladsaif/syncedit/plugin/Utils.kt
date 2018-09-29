package vladsaif.syncedit.plugin

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.io.exists
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import vladsaif.syncedit.plugin.format.ScreencastZipper
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

val RESOURCES_PATH: Path = Paths.get("src", "test", "resources")
val CREDENTIALS_PATH: Path? = System.getProperty("google.credentials")?.let { File(it).toPath() }

fun LightCodeInsightFixtureTestCase.createKtFile(text: String): KtFile {
  return createLightFile("file.kts", KotlinLanguage.INSTANCE, text).cast()
}

fun InputStream.sha1sum(): String {
  val buffer = ByteArray(1 shl 14)
  val summer = MessageDigest.getInstance("SHA-1")
  var read = 0
  while (true) {
    read = read(buffer)
    if (read < 0) {
      break
    }
    summer.update(buffer, 0, read)
  }
  return Base64.getEncoder().encodeToString(summer.digest())
}

fun prepareTestScreencast(project: Project, out: Path, audio: Path?, script: String?, data: TranscriptData?) {
  val builder = ScreencastZipper.createZipBuilder(out)
  if (audio != null) {
    builder.addAudio(audio)
  }
  if (script != null) {
    builder.addScript(script)
  }
  if (data != null) {
    builder.addTranscriptData(data)
  }
  if (!out.exists() || !builder.consistentWith(ScreencastFile(project, out))) {
    println("Cache is not consistent. Recreating: $out")
    builder.zip()
  }
}

fun ScreencastZipper.ZipBuilder.consistentWith(screencast: ScreencastFile): Boolean {
  if (audio != null && screencast.audioInputStream != null) {
    val consistent = Files.newInputStream(audio).use { cached ->
      cached.buffered().sha1sum() == screencast.audioInputStream!!.buffered().sha1sum()
    }
    if (!consistent) return false
  }
  return script == screencast.scriptDocument?.text && data == screencast.data
}