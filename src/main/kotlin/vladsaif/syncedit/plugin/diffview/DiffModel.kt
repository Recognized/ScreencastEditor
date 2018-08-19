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
import kotlin.coroutines.experimental.buildSequence

class DiffModel(
    val origin: MultimediaModel,
    val editor: EditorEx,
    val panel: TextItemPanel
) : ChangeNotifier by DefaultChangeNotifier() {
  private val activeLineHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  var selectedItems: IRange = IRange.EMPTY_RANGE
    set(value) {
      if (field != value) {
        field = value
        var needRedraw = false
        for ((item, height) in itemsWithHeights()) {
          val before = item.isSelected
          item.isSelected = height.intersects(value)
          needRedraw = needRedraw or (before != item.isSelected)
        }
        if (needRedraw) fireStateChanged()
      }
    }
  val textItems: List<TextItem> = panel.components.filterIsInstance(TextItem::class.java)
  var bindings: List<Binding> = listOf()
    set(value) {
      if (field != value) {
        updateHighlighters(field, value)
        field = value
        updateItemBind()
        fireStateChanged()
      }
    }

  private fun updateItemBind() {
    for (x in textItems) x.isBind = false
    for (binding in bindings) {
      for (item in binding.itemRange.toIntRange()) {
        textItems[item].isBind = true
      }
    }
  }

  private fun itemsWithHeights() = buildSequence {
    var sum = 0
    for (component in panel.components) {
      if (component is TextItem) {
        yield(component to IRange(sum, sum + component.height))
      }
      sum += component.height
    }
  }

  private fun updateHighlighters(oldBindings: List<Binding>, newBindings: List<Binding>) {
    val previouslyHighlighted = IRangeUnion()
    val newlyHighlighted = IRangeUnion()
    for (binding in oldBindings) {
      previouslyHighlighted.union(binding.lineRange)
    }
    for (binding in newBindings) {
      newlyHighlighted.union(binding.lineRange)
    }
    for (binding in oldBindings) {
      newlyHighlighted.exclude(binding.lineRange)
    }
    for (binding in newBindings) {
      previouslyHighlighted.exclude(binding.lineRange)
    }
    for ((highlighter, line) in activeLineHighlighters) {
      if (line in previouslyHighlighted) {
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
    activeLineHighlighters.removeAll { it.second in previouslyHighlighted }
    createHighlighters(newlyHighlighted.ranges)
  }

  init {
    createHighlighters(bindings.map { it.lineRange })
    bindings = origin.data!!.bindings
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