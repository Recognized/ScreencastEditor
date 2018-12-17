package com.github.recognized.screencast.editor.model

import com.github.recognized.screencast.editor.*
import com.github.recognized.screencast.editor.format.getSettings
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings
import com.github.recognized.screencast.recorder.format.ScreencastZipper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.io.exists
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class ScreencastTest : LightCodeInsightFixtureTestCase() {

  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().invokeAndWait {
      prepareTestScreencast(
        project,
        PLUGIN_AUDIO_PATH,
        IMPORTED_AUDIO_PATH,
        SETTINGS
      )
    }
    println(SETTINGS)
    println("")
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
      pluginAudio: Path?,
      importedAudio: Path?,
      settings: ScreencastZipSettings
    ) {
      val out = SCREENCAST_PATH
      if (out.exists()) {
        val screencast = Screencast(project, out)
        runBlocking {
          screencast.initialize()
        }
        if (consistentWith(
            pluginAudio = pluginAudio,
            importedAudio = importedAudio,
            settings = settings,
            screencast = screencast
          )
        ) {
          println("Cache is consistent.")
          return
        }
      }
      println("Cache is not consistent. Recreating: $out")
      ScreencastZipper.createZip(out) {
        this.settings = settings
        if (pluginAudio != null) {
          addPluginAudio(Files.newInputStream(pluginAudio))
        }
        if (importedAudio != null) {
          addImportedAudio(Files.newInputStream(importedAudio))
        }
      }
    }

    private fun consistentWith(
      pluginAudio: Path?,
      importedAudio: Path?,
      settings: ScreencastZipSettings,
      screencast: Screencast
    ): Boolean {
      var consistent = isAudioConsistent(pluginAudio, screencast.pluginAudio?.let { { it.audioInputStream } })
      consistent = consistent and
          isAudioConsistent(importedAudio, screencast.importedAudio?.let { { it.audioInputStream } })
      consistent = consistent and (settings == ScreencastZipper.getSettings(screencast))
      return consistent
    }

    private fun isAudioConsistent(audio: Path?, streamSource: (() -> InputStream)?): Boolean {
      if (audio != null && streamSource != null) {
        return Files.newInputStream(audio).use { cached ->
          cached.buffered().use(InputStream::sha1sum) == streamSource().buffered().use(
            InputStream::sha1sum
          )
        }
      } else {
        return (audio == null) == (streamSource == null)
      }
    }
  }
}