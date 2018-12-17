package com.github.recognized.screencast.editor.lang.transcript.psi

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.ScreencastEditorIcons
import java.util.*
import javax.swing.Icon

// Do not create file of this file type manually
object TranscriptFileType : LanguageFileType(TranscriptLanguage) {
  private val extension = UUID.randomUUID().toString()

  override fun getName() = "Transcript file"

  override fun getDescription() = "Transcript PSI Skeleton"

  override fun getDefaultExtension() = extension

  override fun getIcon(): Icon? = ScreencastEditorIcons.TRANSCRIPT
}