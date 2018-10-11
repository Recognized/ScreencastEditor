package vladsaif.syncedit.plugin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.io.exists
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import vladsaif.syncedit.plugin.format.ScreencastFileType
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
val SCREENCAST_PATH: Path = RESOURCES_PATH.resolve("screencast.${ScreencastFileType.defaultExtension}")
val TRANSCRIPT_DATA = TranscriptData(listOf(
    WordData("first", IRange(1000, 2000)),
    WordData("two", IRange(2000, 3000)),
    WordData("three", IRange(3000, 4000)),
    WordData("four", IRange(4000, 5000)),
    WordData("five", IRange(5000, 6000)),
    WordData("six", IRange(6000, 7000)),
    WordData("seven", IRange(8000, 9000)),
    WordData("eight", IRange(9000, 9500)),
    WordData("nine", IRange(10000, 11000)),
    WordData("ten", IRange(11000, 12000)),
    WordData("eleven", IRange(12000, 13000)),
    WordData("twelve", IRange(13000, 14000))
))
val AUDIO_PATH: Path = RESOURCES_PATH.resolve("demo.wav")
val SCRIPT_TEXT =
    """|timeOffset(ms = 1000L)
       |ideFrame {
       |    invokeAction("vladsaif.syncedit.plugin.OpenDiff")
       |    timeOffset(1000L)
       |    editor {
       |        timeOffset(1000L)
       |        typeText(""${'"'}some text""${'"'})
       |        timeOffset(1000L)
       |        typeText(""${'"'}typing""${'"'})
       |    }
       |    timeOffset(1000L)
       |    toolsMenu {
       |        timeOffset(ms = 1000L)
       |        item("ScreencastEditor").click()
       |        timeOffset(1000L)
       |        chooseFile {
       |            timeOffset(1000L)
       |            button("Ok").click()
       |        }
       |        timeOffset(1000L)
       |    }
       |    timeOffset(1000L)
       |}""".trimIndent().trimMargin()

fun createScriptPsi(project: Project): PsiFile {
  return PsiFileFactory.getInstance(project).createFileFromText(
      "demo.kts",
      KotlinFileType.INSTANCE,
      SCRIPT_TEXT,
      0,
      true,
      false
  )
}

fun LightCodeInsightFixtureTestCase.createKtFile(text: String): KtFile {
  return createLightFile("file.kts", KotlinLanguage.INSTANCE, text).cast()
}

fun InputStream.sha1sum(): String {
  val buffer = ByteArray(1 shl 14)
  val summer = MessageDigest.getInstance("SHA-1")
  var read: Int
  while (true) {
    read = read(buffer)
    if (read < 0) {
      break
    }
    summer.update(buffer, 0, read)
  }
  return Base64.getEncoder().encodeToString(summer.digest())
}

fun prepareTestScreencast(
    project: Project,
    audio: Path?,
    script: String?,
    data: TranscriptData?
) {
  val out = SCREENCAST_PATH
  if (out.exists()) {
    val screencast = ScreencastFile(project, out)
    runBlocking {
      screencast.initialize()
    }
    if (consistentWith(audio, script, data, screencast)) {
      println("Cache is consistent.")
      return
    }
  }
  println("Cache is not consistent. Recreating: $out")
  ScreencastZipper(out).use { zipper ->
    if (audio != null) {
      zipper.addAudio(audio)
    }
    if (script != null) {
      zipper.addScript(script)
    }
    if (data != null) {
      zipper.addTranscriptData(data)
    }
  }
}

private fun consistentWith(
    audio: Path?,
    script: String?,
    data: TranscriptData?,
    screencast: ScreencastFile
): Boolean {
  if (audio != null && screencast.isAudioSet) {
    val consistent = Files.newInputStream(audio).use { cached ->
      cached.buffered().use(InputStream::sha1sum) == screencast.audioInputStream.buffered().use(InputStream::sha1sum)
    }
    if (!consistent) return false
  }
  return script == screencast.scriptDocument?.text && data == screencast.data
}