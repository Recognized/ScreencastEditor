package vladsaif.syncedit.plugin.model

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ScreencastFileTest : LightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    prepareTestScreencast(project, AUDIO_PATH, createScriptPsi(project).text, DefaultEditionModel(), TRANSCRIPT_DATA)
  }

  private fun withModel(block: ScreencastFile.() -> Unit) {
    val model = runBlocking {
      ScreencastFile.create(project, SCREENCAST_PATH)
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
      assertNotNull(audioDataModel)
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
        editionModel.cut(coordinator.toFrameRange(IntRange(900, 2100), TimeUnit.MILLISECONDS))
        editionModel.mute(coordinator.toFrameRange(IntRange(2900, 4100), TimeUnit.MILLISECONDS))
      }
      assertEquals(TRANSCRIPT_DATA.delete(0).mute(IntArray(1) { 1 }), data!!)
    }
  }

  @Test
  fun `test light save function`() {
    withModel {
      performModification {
        editionModel.cut(coordinator.toFrameRange(IntRange(900, 2100), TimeUnit.MILLISECONDS))
        editionModel.mute(coordinator.toFrameRange(IntRange(2900, 4100), TimeUnit.MILLISECONDS))
      }
      val func = getLightSaveFunction()
      val out = Paths.get("screencastSaved.scs")
      func(out)
      val saved = runBlocking { ScreencastFile.create(project, out) }
      try {
        assertEquals(this.data, saved.data)
        assertEquals(this.editionModel, saved.editionModel)
        assertEquals(this.scriptDocument?.text, saved.scriptDocument?.text)
      } finally {
        Files.deleteIfExists(out)
        saved.dispose()
      }
    }
  }
}