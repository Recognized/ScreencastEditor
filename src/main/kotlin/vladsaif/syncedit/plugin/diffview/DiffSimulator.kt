package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.TextDiffType
import com.intellij.openapi.editor.Editor
import vladsaif.syncedit.plugin.Settings

object DiffSimulator : TextDiffType {

  override fun getMarkerColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR

  override fun getName() = "Screencast Editor binding highlighter"

  override fun getColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR

  override fun getIgnoredColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR
}
