package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.TextDiffType
import com.intellij.openapi.editor.Editor
import vladsaif.syncedit.plugin.ColorSettings

object MappedLine : TextDiffType {

  override fun getMarkerColor(editor: Editor?) = ColorSettings.MAPPING_HIGHLIGHT_COLOR

  override fun getName() = "Screencast Editor binding highlighter"

  override fun getColor(editor: Editor?) = ColorSettings.MAPPING_HIGHLIGHT_COLOR

  override fun getIgnoredColor(editor: Editor?) = ColorSettings.MAPPING_HIGHLIGHT_COLOR
}
