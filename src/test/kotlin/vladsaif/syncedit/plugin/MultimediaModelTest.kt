package vladsaif.syncedit.plugin

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.junit.Test
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType

class MultimediaModelTest : LightCodeInsightFixtureTestCase() {

  private fun withModel(block: MultimediaModel.() -> Unit) {
    val model = MultimediaModel(project)
    model.block()
  }

  private val testTranscriptData = TranscriptData(listOf(
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

  private val testScript by lazy {
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

  private val testXml by lazy {
    PsiFileFactory.getInstance(project).createFileFromText(
        "demo.${InternalFileType.defaultExtension}",
        InternalFileType,
        testTranscriptData.toXml(),
        0L,
        true
    )
  }

  private val testAudio by lazy {
    VirtualFileManager.getInstance().findFileByUrl(RESOURCES_PATH.resolve("demo.wav").toUri().toASCIIString())!!
  }

  @Test
  fun `test audio model set`() {
    withModel {
      audioFile = testAudio
      assertNotNull(audioDataModel)
    }
  }

  @Test
  fun `test psi files available`() {
    withModel {
      setAndReadXml(testXml.virtualFile)
      scriptFile = testScript.virtualFile!!

      assertNotNull(xmlFile)
      assertNotNull(scriptPsi)
      assertNotNull(scriptDoc)
    }
  }

  @Test
  fun `test changes applied during recognition`() {
    withModel {
      audioFile = testAudio
      editionModel.cut(audioDataModel!!.msRangeToFrameRange(IRange(900, 2100)))
      editionModel.mute(audioDataModel!!.msRangeToFrameRange(IRange(2900, 4100)))
      setAndReadXml(testXml.virtualFile)
      assertEquals(data!!, testTranscriptData.excludeWord(0).muteWords(IntArray(1) { 2 }))
    }
  }

  @Test
  fun `test default binding`() {
    withModel {
      data = testTranscriptData
      scriptFile = testScript.virtualFile
      createDefaultBinding()
      data!!.words.forEach(::println)
      // TODO
    }
  }
}