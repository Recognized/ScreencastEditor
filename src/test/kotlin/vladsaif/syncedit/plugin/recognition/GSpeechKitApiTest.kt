package vladsaif.syncedit.plugin.recognition

import org.junit.Assume
import org.junit.Before
import org.junit.Test
import vladsaif.syncedit.plugin.CREDENTIALS_PATH
import vladsaif.syncedit.plugin.RESOURCES_PATH
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.recognition.recognizers.GSpeechKit
import kotlin.test.assertTrue

class GSpeechKitApiTest {

  @Before
  fun before() {
    Assume.assumeTrue(
        "Credentials for Google Speech-to-text API are not available.",
        CREDENTIALS_PATH != null
    )
    GCredentialProvider.Instance.setGCredentialsFile(CREDENTIALS_PATH!!)
  }

  @Test
  fun `test wav recognition`() {
    val future = GSpeechKit().recognize(RESOURCES_PATH.resolve("demo.wav"))
    testResult(future.get())
  }

  @Test
  fun `test mp3 recognition`() {
    val future = GSpeechKit().recognize(RESOURCES_PATH.resolve("demo.mp3"))
    testResult(future.get())
  }

  private fun testResult(data: TranscriptData) {
    assertTrue("No words recognized.") {
      data.words.isNotEmpty()
    }
    assertTrue("Some words have empty time range.") {
      data.words.all { !it.range.empty }
    }
    assertTrue("Words are overlapping.") {
      data.words.asSequence().zipWithNext().all { (current, next) -> current.range.end <= next.range.start }
    }
  }
}