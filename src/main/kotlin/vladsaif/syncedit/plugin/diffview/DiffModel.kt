package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.DiffDrawUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.RangeHighlighter
import vladsaif.syncedit.plugin.Binding
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.IRangeUnion
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier

class DiffModel(
    val origin: MultimediaModel,
    val editor: EditorEx
) : ChangeNotifier by DefaultChangeNotifier() {
  private val activeLineHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  var bindings: List<Binding> = origin.data!!.bindings
    set(value) {
      if (field != value) {
        val previouslyHighlighted = IRangeUnion()
        val newlyHighlighted = IRangeUnion()
        for (binding in field) {
          previouslyHighlighted.union(binding.lineRange)
        }
        for (binding in value) {
          newlyHighlighted.union(binding.lineRange)
        }
        for (binding in field) {
          newlyHighlighted.exclude(binding.lineRange)
        }
        for (binding in value) {
          previouslyHighlighted.exclude(binding.lineRange)
        }
        for ((highlighter, line) in activeLineHighlighters) {
          if (line in previouslyHighlighted) {
            editor.markupModel.removeHighlighter(highlighter)
          }
        }
        activeLineHighlighters.removeAll { it.second in previouslyHighlighted }
        createHighlighters(newlyHighlighted.ranges)
        fireStateChanged()
      }
    }

  init {
    createHighlighters(bindings.map { it.lineRange })
  }

  private fun createHighlighters(lines: List<IRange>) {
    for (line in lines) {
      val highlighters = editor.createHighlighter(line)
      for ((x, index) in highlighters.zip(line.toIntRange())) {
        activeLineHighlighters.add(x to index)
      }
    }
  }

  companion object {

    private fun Editor.createHighlighter(line: IRange): List<RangeHighlighter> {
      return DiffDrawUtil.createHighlighter(this, line.start, line.end + 1, DiffSimulator, false)
    }
  }
}