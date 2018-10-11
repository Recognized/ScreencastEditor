package vladsaif.syncedit.plugin

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class ScreencastFileTest : LightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    prepareTestScreencast(project, AUDIO_PATH, createScriptPsi(project).text, TRANSCRIPT_DATA)
  }

  private fun withModel(block: ScreencastFile.() -> Unit) {
    val model = runBlocking {
      ScreencastFile.create(project, SCREENCAST_PATH)
    }
    model.block()
    model.dispose()
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
  fun `test default binding`() {
    withModel {
      data = TRANSCRIPT_DATA
      createDefaultBinding()
      data!!.words.forEach(::println)
      // TODO
    }
  }
}