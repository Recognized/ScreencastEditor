package vladsaif.syncedit.plugin.model

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.io.exists
import kotlinx.coroutines.runBlocking
import org.junit.Test
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.format.ScreencastZipper
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ScreencastTest : LightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    prepareTestScreencast(project, PLUGIN_AUDIO_PATH, SETTINGS)
  }

  private fun withModel(block: Screencast.() -> Unit) {
    val model = runBlocking {
      Screencast.create(project, SCREENCAST_PATH)
    }
    try {
      model.block()
    } finally {
      model.dispose()
    }
  }

  @Test
  fun `test audio model set`() {
    withModel {
      assertNotNull(pluginAudio)
    }
  }

  @Test
  fun `test psi files available`() {
    withModel {
      assertNotNull(scriptPsi)
      assertNotNull(scriptDocument)
    }
  }

  @Test
  fun `test changes applied during recognition`() {
    withModel {
      performModification {
        pluginEditableAudio!!.editionsModel.cut(coordinator.toFrameRange(900..2100, TimeUnit.MILLISECONDS))
        pluginEditableAudio!!.editionsModel.mute(coordinator.toFrameRange(2900..4100, TimeUnit.MILLISECONDS))
      }
      assertEquals(PLUGIN_TRANSCRIPT_DATA.delete(0).mute(IntArray(1) { 1 }), pluginAudio!!.data!!)
    }
  }

  @Test
  fun `test light save function`() {
    withModel {
      performModification {
        pluginEditableAudio!!.editionsModel.cut(coordinator.toFrameRange(900..2100, TimeUnit.MILLISECONDS))
        pluginEditableAudio!!.editionsModel.mute(coordinator.toFrameRange(2900..4100, TimeUnit.MILLISECONDS))
      }
      val func = getLightSaveFunction()
      val out = Paths.get("screencastSaved.scs")
      func(out)
      val saved = runBlocking { Screencast.create(project, out) }
      try {
        assertEquals(ScreencastZipper.getSettings(this), ScreencastZipper.getSettings(saved))
      } finally {
        Files.deleteIfExists(out)
        saved.dispose()
      }
    }
  }

  companion object {

    fun prepareTestScreencast(
      project: Project,
      audio: Path?,
      settings: ScreencastZipper.Settings
    ) {
      val out = SCREENCAST_PATH
      if (out.exists()) {
        val screencast = Screencast(project, out)
        runBlocking {
          screencast.initialize()
        }
        if (consistentWith(audio, settings, screencast)) {
          println("Cache is consistent.")
          return
        }
      }
      println("Cache is not consistent. Recreating: $out")
      ScreencastZipper.createZip(out) {
        setSettings(settings)
        if (audio != null) {
          addPluginAudio(Files.newInputStream(audio))
        }
      }
    }

    fun consistentWith(
      audio: Path?,
      settings: ScreencastZipper.Settings,
      screencast: Screencast
    ): Boolean {
      if (audio != null && screencast.pluginAudio != null) {
        val consistent = Files.newInputStream(audio).use { cached ->
          cached.buffered().use(InputStream::sha1sum) == screencast.pluginAudio!!.audioInputStream.buffered().use(
            InputStream::sha1sum
          )
        }
        if (!consistent) return false
      }
      return settings == ScreencastZipper.getSettings(screencast)
    }
  }
}