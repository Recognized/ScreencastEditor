package com.github.recognized.screencast.editor.lang.transcript.psi

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType

object InternalFileType : LanguageFileType(XMLLanguage.INSTANCE) {
  override fun getIcon() = XmlFileType.INSTANCE.icon

  override fun getName() = "Transcript"

  override fun getDefaultExtension() = "transcript"

  override fun getDescription() = "Transcript file type"
}