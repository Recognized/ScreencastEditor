package com.github.recognized.screencast.editor.format

import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.editor.model.TranscriptData
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings
import com.github.recognized.screencast.recorder.format.ScreencastZipper
import com.intellij.openapi.application.ApplicationManager
import java.nio.file.Path

class TranscriptDataKey(name: String) : ScreencastZipSettings.Key<TranscriptData>(name) {
  override fun deserialize(obj: String): TranscriptData {
    return TranscriptData.createFrom(obj)
  }

  override fun serialize(value: TranscriptData): String {
    return value.toXml()
  }
}

val IMPORTED_TRANSCRIPT_DATA_KEY = TranscriptDataKey("IMPORTED_TRANSCRIPT_DATA")
val PLUGIN_TRANSCRIPT_DATA_KEY = TranscriptDataKey("PLUGIN_TRANSCRIPT_DATA")

fun ScreencastZipper.zipLight(screencast: Screencast, out: Path) {
  ScreencastZipper.createZip(out) {
    ApplicationManager.getApplication().invokeAndWait {
      settings = getSettings(screencast)
    }
    screencast.pluginAudio?.let { audio ->
      addPluginAudio(audio.audioInputStream)
    }
    screencast.importedAudio?.let { audio ->
      addImportedAudio(audio.audioInputStream)
    }
  }
}

fun ScreencastZipper.getSettings(screencast: Screencast): ScreencastZipSettings {
  with(screencast) {
    return ScreencastZipSettings().apply {
      set(ScreencastZipSettings.IMPORTED_AUDIO_OFFSET_KEY, importedAudio?.model?.offsetFrames)
      set(ScreencastZipSettings.IMPORTED_EDITIONS_VIEW_KEY, importedAudio?.editionsModel)
      set(IMPORTED_TRANSCRIPT_DATA_KEY, importedAudio?.data)
      set(ScreencastZipSettings.PLUGIN_AUDIO_OFFSET_KEY, pluginAudio?.model?.offsetFrames)
      set(ScreencastZipSettings.PLUGIN_EDITIONS_VIEW_KEY, pluginAudio?.editionsModel)
      set(PLUGIN_TRANSCRIPT_DATA_KEY, pluginAudio?.data)
      set(ScreencastZipSettings.SCRIPT_KEY, screencast.codeModel.serialize())
    }
  }
}