package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.DiffDrawUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import java.awt.Font
import kotlin.coroutines.experimental.buildSequence

class DiffModel(
    val origin: MultimediaModel,
    val editor: EditorEx,
    val panel: TextItemPanel
) : ChangeNotifier by DefaultChangeNotifier() {
  private val myActiveLineHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  private var mySelectionRangeHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  var hoveredItem = -1
    set(value) {
      if (field != value) {
        if (field >= 0) textItems[field].isHovered = false
        if (value >= 0) textItems[value].isHovered = true
        field = value
        fireStateChanged()
      }
    }
  var editorSelectionRange: IRange = IRange.EMPTY_RANGE
    set(value) {
      if (field != value) {
        val removal = IRangeUnion()
        val addition = IRangeUnion()
        removal.union(field)
        removal.exclude(value)
        addition.union(value)
        addition.exclude(field)
        for ((range, line) in mySelectionRangeHighlighters) {
          if (line in removal) editor.markupModel.removeHighlighter(range)
        }
        mySelectionRangeHighlighters.removeAll { it.second in removal }
        for (line in addition.ranges.flatMap { it.toIntRange() }) {
          mySelectionRangeHighlighters.add(editor.markupModel.addLineHighlighter(
              line,
              HighlighterLayer.SELECTION + 1,
              TextAttributes(null, Settings.DIFF_SELECTED_COLOR, null, EffectType.BOXED, Font.TYPE1_FONT)
          ) to line)
        }
        field = value
        fireStateChanged()
      }
    }
  var selectedItems: IRange = IRange.EMPTY_RANGE
    set(value) {
      if (field != value) {
        field = value
        var needRedraw = false
        for ((index, item) in textItems.withIndex()) {
          val before = item.isSelected
          item.isSelected = index in value
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

  fun selectHeightRange(heightRange: IRange) {
    selectedItems = toItemRange(heightRange)
  }

  private fun toItemRange(heightRange: IRange): IRange {
    var start = -1
    var end = -2
    for ((index, pair) in itemsWithHeights().withIndex()) {
      if (pair.second.intersects(heightRange)) {
        if (start == -1) start = index
        end = index
      } else if (start != -1) break
    }
    return IRange(start, end)
  }

  private fun updateItemBind() {
    for (x in textItems) {
      x.isBind = false
      x.isDrawBottomBorder = false
      x.isDrawTopBorder = false
    }
    for (binding in bindings) {
      val range = binding.itemRange
      textItems[range.start].isDrawTopBorder = true
      textItems[range.end].isDrawBottomBorder = true
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
    for ((highlighter, line) in myActiveLineHighlighters) {
      if (line in previouslyHighlighted) {
        editor.markupModel.removeHighlighter(highlighter)
      }
    }
    myActiveLineHighlighters.removeAll { it.second in previouslyHighlighted }
    createHighlighters(newlyHighlighted.ranges)
  }

  init {
    createHighlighters(bindings.map { it.lineRange })
    editor.selectionModel.addSelectionListener { editorSelectionUpdated() }
    bindings = origin.data!!.bindings
  }

  private fun editorSelectionUpdated() {
    val startLine = editor.offsetToLogicalPosition(editor.selectionModel.selectionStart).line
    val endLine = editor.offsetToLogicalPosition(editor.selectionModel.selectionEnd).line
    val selectedRange = if (editor.selectionModel.hasSelection()) IRange(startLine, endLine) else IRange.EMPTY_RANGE
    editorSelectionRange = selectedRange
  }

  private fun createHighlighters(lines: List<IRange>) {
    for (line in lines) {
      val highlighters = editor.createHighlighter(line)
      for ((x, index) in highlighters.zip(line.toIntRange())) {
        myActiveLineHighlighters.add(x to index)
      }
    }
  }

  companion object {

    private fun Editor.createHighlighter(line: IRange): List<RangeHighlighter> {
      return DiffDrawUtil.createHighlighter(this, line.start, line.end + 1, DiffSimulator, false)
    }
  }
}