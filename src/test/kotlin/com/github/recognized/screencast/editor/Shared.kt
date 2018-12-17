package com.github.recognized.screencast.editor

import com.github.recognized.screencast.editor.format.IMPORTED_TRANSCRIPT_DATA_KEY
import com.github.recognized.screencast.editor.format.PLUGIN_TRANSCRIPT_DATA_KEY
import com.github.recognized.screencast.editor.lang.script.psi.codeModel
import com.github.recognized.screencast.editor.model.TranscriptData
import com.github.recognized.screencast.editor.model.WordData
import com.github.recognized.screencast.recorder.format.ScreencastFileType
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.IMPORTED_AUDIO_OFFSET_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.IMPORTED_EDITIONS_VIEW_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.PLUGIN_AUDIO_OFFSET_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.PLUGIN_EDITIONS_VIEW_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.SCRIPT_KEY
import com.github.recognized.screencast.recorder.sound.impl.DefaultEditionsModel
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

val RESOURCES_PATH: Path = Paths.get("src", "test", "resources")
val CREDENTIALS_PATH: Path? = System.getProperty("google.credentials")?.let { File(it).toPath() }
val SCREENCAST_PATH: Path = RESOURCES_PATH.resolve("screencast.${ScreencastFileType.defaultExtension}")
val PLUGIN_TRANSCRIPT_DATA = TranscriptData.create(
  listOf(
    "first" to 1000..2000,
    "two" to 2000..3000,
    "three" to 3000..4000,
    "four" to 4000..5000,
    "five" to 5000..6000,
    "six" to 6000..7000,
    "seven" to 8000..9000,
    "eight" to 9000..9500,
    "nine" to 10000..11000,
    "ten" to 11000..12000,
    "eleven" to 12000..13000,
    "twelve" to 13000..14000
  ).map { (a, b) -> WordData(a, b) }
)
val PLUGIN_EDITIONS_MODEL get() = DefaultEditionsModel()
val IMPORTED_EDITIONS_MODEL get() = DefaultEditionsModel()
val PLUGIN_AUDIO_PATH: Path = RESOURCES_PATH.resolve("demo.wav")
const val IMPORTED_AUDIO_OFFSET = 200L
const val PLUGIN_AUDIO_OFFSET = -100L
val IMPORTED_AUDIO_PATH: Path = RESOURCES_PATH.resolve("demo.mp3")
val CODE_MODEL = codeModel {
  block("ideFrame", 1000..12000) {
    statement("invokeAction(\"vladsaif.syncedit.plugin.OpenDiff\")", 1000)
    block("editor", 2000..4000) {
      statement("type1", 3000)
      statement("type2", 4000)
    }
    block("toolsMenu", 5000..10000) {
      statement("click", 6000)
      block("chooseFile", 7000..8000) {
        statement("button.click()", 8000)
      }
    }
  }
}
val SETTINGS = ScreencastZipSettings().apply {
  set(IMPORTED_AUDIO_OFFSET_KEY, IMPORTED_AUDIO_OFFSET)
  set(IMPORTED_EDITIONS_VIEW_KEY, IMPORTED_EDITIONS_MODEL)
  set(IMPORTED_TRANSCRIPT_DATA_KEY, PLUGIN_TRANSCRIPT_DATA)
  set(PLUGIN_AUDIO_OFFSET_KEY, PLUGIN_AUDIO_OFFSET)
  set(PLUGIN_EDITIONS_VIEW_KEY, PLUGIN_EDITIONS_MODEL)
  set(PLUGIN_TRANSCRIPT_DATA_KEY, PLUGIN_TRANSCRIPT_DATA)
  set(SCRIPT_KEY, CODE_MODEL.serialize())
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