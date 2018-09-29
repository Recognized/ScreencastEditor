package vladsaif.syncedit.plugin

import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.idea.KotlinFileType
import org.junit.Before
import org.junit.Test
import vladsaif.syncedit.plugin.format.ScreencastFileType

class ScreencastFileTest : LightCodeInsightFixtureTestCase() {
  private val screencastPath = RESOURCES_PATH.resolve(this.javaClass.name + ScreencastFileType.defaultExtension)
  private val transcriptData = TranscriptData(listOf(
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
  private val audioPath = RESOURCES_PATH.resolve("demo.wav")
  private val script by lazy {
    PsiFileFactory.getInstance(project).createFileFromText(
        "demo.kts",
        KotlinFileType.INSTANCE,
        """
        |timeOffset(ms = 1000L)
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
        |}""".trimIndent().trimMargin(),
        0L,
        true
    )
  }

  @Before
  fun before() {
    prepareTestScreencast(project, screencastPath, audioPath, script.text, transcriptData)
  }

  private fun withModel(block: ScreencastFile.() -> Unit) {
    val model = runBlocking {
      ScreencastFile.create(project, screencastPath)
    }
    model.block()
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
      assertEquals(data!!, transcriptData.excludeWord(0).muteWords(IntArray(1) { 2 }))
    }
  }

  @Test
  fun `test default binding`() {
    withModel {
      data = transcriptData
      createDefaultBinding()
      data!!.words.forEach(::println)
      // TODO
    }
  }
}