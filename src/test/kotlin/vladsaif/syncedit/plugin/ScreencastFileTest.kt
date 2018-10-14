package vladsaif.syncedit.plugin

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultEditionModel
import java.nio.file.Files
import java.nio.file.Paths

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
      editionModel.cut(audioDataModel!!.msRangeToFrameRange(IRange(900, 2100)))
      editionModel.mute(audioDataModel!!.msRangeToFrameRange(IRange(2900, 4100)))
      assertEquals(data!!, TRANSCRIPT_DATA.excludeWord(0).muteWords(IntArray(1) { 2 }))
    }
  }

  @Test
  fun `test light save function`() {
    withModel {
      editionModel.cut(audioDataModel!!.msRangeToFrameRange(IRange(900, 2100)))
      editionModel.mute(audioDataModel!!.msRangeToFrameRange(IRange(2900, 4100)))
      val func = getLightSaveFunction()
      val out = Paths.get("screencastSaved.scs")
      func(out)
      val saved = runBlocking { ScreencastFile.create(project, out) }
      try {
        assertEquals(this.data, saved.data)
        assertEquals(this.editionModel, saved.editionModel)
        assertEquals(this.scriptDocument?.text, saved.scriptDocument?.text)
        Files.deleteIfExists(out)
      } finally {
        saved.dispose()
      }
    }
  }

  @Test
  fun `test default binding`() {
    withModel {
      loadTranscriptData(TRANSCRIPT_DATA)
      createDefaultBinding()
      data!!.words.forEach(::println)
      // TODO
    }
  }
}