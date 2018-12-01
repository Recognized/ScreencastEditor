package vladsaif.syncedit.plugin.format

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import vladsaif.syncedit.plugin.PLUGIN_AUDIO_PATH
import vladsaif.syncedit.plugin.SETTINGS
import vladsaif.syncedit.plugin.model.Screencast
import vladsaif.syncedit.plugin.sha1sum
import java.nio.file.Files

class ScreencastZipperTest : LightCodeInsightFixtureTestCase() {

  private val myTempFile = Files.createTempFile(
    this.javaClass.name.replace('\\', '.'), "" +
        ".${ScreencastFileType.defaultExtension}"
  )
  private val myScreencast by lazy {
    ScreencastZipper.createZip(myTempFile) {
      addPluginAudio(Files.newInputStream(PLUGIN_AUDIO_PATH))
      setSettings(SETTINGS)
    }
    val screencast = Screencast(project, myTempFile)
    runBlocking {
      screencast.initialize()
    }
    screencast
  }

  fun `test screencast is consistent`() {
    assertEquals(SETTINGS, ScreencastZipper.getSettings(myScreencast))
    assertEquals(
      Files.newInputStream(PLUGIN_AUDIO_PATH).buffered().sha1sum(),
      myScreencast.pluginAudio!!.audioInputStream.sha1sum()
    )
  }

  @After
  fun after() {
    Files.delete(myTempFile)
  }
}