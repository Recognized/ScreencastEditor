package com.github.recognized.screencast.editor.recognition

import com.github.recognized.screencast.editor.CREDENTIALS_PATH
import com.github.recognized.screencast.editor.RESOURCES_PATH
import com.github.recognized.screencast.editor.model.TranscriptData
import com.github.recognized.screencast.editor.recognition.recognizers.GSpeechKit
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class GSpeechKitApiTest {

  @Before
  fun before() {
    Assume.assumeTrue(
      "Credentials for Google Speech-to-text API are not available.",
      CREDENTIALS_PATH != null && CREDENTIALS_PATH.toString() != ""
    )
    CredentialsProvider.setCredentials(CREDENTIALS_PATH!!)
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
      data.words.all { !it.range.isEmpty() }
    }
    assertTrue("Words are overlapping.") {
      data.words.asSequence().zipWithNext().all { (current, next) -> current.range.endInclusive <= next.range.start }
    }
  }
}