package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.DiffDrawUtil
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.MouseDragListener
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.max
import kotlin.math.min

class DiffModel(
    val origin: MultimediaModel,
    val editor: EditorEx,
    val panel: TextItemPanel
) : ChangeNotifier by DefaultChangeNotifier() {
  private val myActiveLineHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  private val myDefaultScheme = DefaultColorsScheme()
  private val myEditorHoveredAttributes = TextAttributes()
  private val myEditorSelectionAttributes = TextAttributes()
  private var myHoveredHighlighter: RangeHighlighter? = null
  private var myIgnoreSelectionEvents = false
  private var mySelectionRangeHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  private val myTextItems: List<TextItem> = panel.components.filterIsInstance(TextItem::class.java)

  private val myEditorDragListener = EditorMouseDragAdapter(object : MouseDragListener() {
    override fun onDrag(point: Point) {
      val event = dragStartEvent ?: return
      val startLine = editor.xyToLogicalPosition(event.point).line
      val endLine = editor.xyToLogicalPosition(point).line
      val selectedRange = IRange(min(startLine, endLine), max(startLine, endLine))
      editorHoveredLine = -1
      editorSelectionRange = selectedRange
    }

    override fun mouseMoved(e: MouseEvent?) {
      super.mouseMoved(e)
      e ?: return
      val line = editor.xyToLogicalPosition(e.point).line
      editorHoveredLine = line
    }

    override fun mouseClicked(e: MouseEvent?) {
      super.mouseClicked(e)
      e ?: return
      val line = editor.xyToLogicalPosition(e.point).line
      editorHoveredLine = -1
      editorSelectionRange = IRange(line, line)
    }

    override fun mousePressed(e: MouseEvent?) {
      super.mousePressed(e)
      mouseClicked(e)
    }

    override fun mouseExited(e: MouseEvent?) {
      super.mouseExited(e)
      editorHoveredLine = -1
    }
  })

  var bindings: List<Binding> = listOf()
    set(value) {
      if (field != value) {
        updateHighlighters(field, value)
        field = value
        updateItemBind()
        fireStateChanged()
      }
    }

  var hoveredItem = -1
    set(value) {
      if (field != value) {
        if (field >= 0) myTextItems[field].isHovered = false
        if (value >= 0) myTextItems[value].isHovered = true
        field = value
        fireStateChanged()
      }
    }

  init {
    myEditorHoveredAttributes.copyFrom(myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!)
    myEditorHoveredAttributes.backgroundColor = Settings.DIFF_HOVERED_COLOR
    myEditorSelectionAttributes.copyFrom(myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!)
    myEditorSelectionAttributes.backgroundColor = Settings.DIFF_SELECTED_COLOR
    createHighlighters(bindings.map { it.lineRange })
    editor.selectionModel.addSelectionListener { editorSelectionUpdated() }
    editor.addEditorMouseMotionListener(myEditorDragListener)
    editor.addEditorMouseListener(myEditorDragListener)
    bindings = origin.data!!.bindings
  }

  // Do not use default selection
  private fun editorSelectionUpdated() {
    if (myIgnoreSelectionEvents) return
    myIgnoreSelectionEvents = true
    editor.selectionModel.removeSelection()
    myIgnoreSelectionEvents = false
    return
  }

  var editorHoveredLine: Int = -1
    set(value) {
      val newLine = max(min(editor.document.lineCount - 1, value), 0)
      if (field != value) {
        val x = myHoveredHighlighter
        if (x != null) {
          editor.markupModel.removeHighlighter(x)
        }
        myHoveredHighlighter = null
        if (value != -1 && value !in editorSelectionRange) {
          val attributes = myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!
          attributes.backgroundColor = Settings.DIFF_HOVERED_COLOR
          myHoveredHighlighter = editor.markupModel.addLineHighlighter(
              value,
              HighlighterLayer.SELECTION,
              myEditorHoveredAttributes
          )
        }
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
              myEditorSelectionAttributes
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
        for ((index, item) in myTextItems.withIndex()) {
          val before = item.isSelected
          item.isSelected = index in value
          needRedraw = needRedraw or (before != item.isSelected)
        }
        if (needRedraw) fireStateChanged()
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
    for (x in myTextItems) {
      x.isBind = false
      x.isDrawBottomBorder = false
      x.isDrawTopBorder = false
    }
    for (binding in bindings) {
      val range = binding.itemRange
      myTextItems[range.start].isDrawTopBorder = true
      myTextItems[range.end].isDrawBottomBorder = true
      for (item in binding.itemRange.toIntRange()) {
        myTextItems[item].isBind = true
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