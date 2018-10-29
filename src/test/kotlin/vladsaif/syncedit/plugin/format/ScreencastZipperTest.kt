package vladsaif.syncedit.plugin.format

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.io.inputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.sound.SoundProvider
import java.nio.file.Files

class ScreencastZipperTest : LightCodeInsightFixtureTestCase() {

  private val myTempFile = Files.createTempFile(
      this.javaClass.name.replace('\\', '.'), "" +
      ".${ScreencastFileType.defaultExtension}"
  )
  private val myScreencast by lazy {
    ScreencastZipper(myTempFile).use {
      it.addScript(SCRIPT_TEXT)
      it.addTranscriptData(TRANSCRIPT_DATA)
      it.addAudio(AUDIO_PATH)
      it.addEditionModel(EDITION_MODEL)
    }
    val screencast = ScreencastFile(project, myTempFile)
    runBlocking {
      screencast.initialize()
    }
    screencast
  }

//  fun `test script preserved`() {
//    assertEquals(SCRIPT_TEXT, myScreencast.scriptDocument!!.text)
//  }

  fun `test transcript preserved`() {
    assertEquals(TRANSCRIPT_DATA, myScreencast.data!!)
  }

  fun `test audio file preserved`() {
    assertEquals(AUDIO_PATH.inputStream().sha1sum(), myScreencast.audioInputStream.sha1sum())
  }

  fun `test audio data preserved`() {
    assertEquals(
        SoundProvider.getAudioInputStream(AUDIO_PATH.toFile()).sha1sum(),
        SoundProvider.getAudioInputStream(myScreencast.audioInputStream).sha1sum()
    )
  }

  fun `test edition model preserved`() {
    assertEquals(EDITION_MODEL, myScreencast.editionModel)
  }

  @After
  fun after() {
    Files.delete(myTempFile)
  }
}