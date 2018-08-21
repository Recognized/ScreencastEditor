package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.TextDiffType
import com.intellij.openapi.editor.Editor
import vladsaif.syncedit.plugin.Settings
import java.awt.Color

object DiffSimulator : TextDiffType {

  override fun getMarkerColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR

  override fun getName() = "Screencast Editor binding highlighter"

  override fun getColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR

  override fun getIgnoredColor(editor: Editor?) = Settings.DIFF_FILLER_COLOR
}

infix fun Color.over(other: Color): Color {
  assert(other.alpha == 255)
  val srcRed = this.red * this.alpha / 255
  val srcGreen = this.green * this.alpha / 255
  val srcBlue = this.blue * this.alpha / 255
  val dstRed = other.red * (255 - this.alpha) / 255
  val dstGreen = other.green * (255 - this.alpha) / 255
  val dstBlue = other.blue * (255 - this.alpha) / 255
  return Color(srcRed + dstRed, srcGreen + dstGreen, srcBlue + dstBlue)
}