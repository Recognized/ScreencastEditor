package vladsaif.syncedit.plugin.diffview

import com.intellij.diff.util.DiffDrawUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.impl.DefaultColorsScheme
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import vladsaif.syncedit.plugin.Binding
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.IRangeUnion
import vladsaif.syncedit.plugin.Settings
import vladsaif.syncedit.plugin.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.MouseDragListener
import java.awt.Point
import java.awt.event.MouseEvent
import kotlin.coroutines.experimental.buildSequence
import kotlin.math.max
import kotlin.math.min

class DiffViewModel(
    private val diffModel: DiffModel,
    val editor: EditorEx,
    private val panel: TextItemPanel
) : ChangeNotifier by DefaultChangeNotifier(), Disposable {
  private val myActiveLineHighlighters: MutableList<Pair<RangeHighlighter, Int>> = mutableListOf()
  private val myDefaultScheme = DefaultColorsScheme()
  private val myEditorHoveredAttributes = TextAttributes()
  private val myEditorSelectionAttributes = TextAttributes()
  private var myHoveredHighlighter: RangeHighlighter? = null
  private var myIgnoreSelectionEvents = false
  private val myLineRange = IRange(0, editor.document.lineCount - 1)
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

  init {
    diffModel.addBindingsListener(this::onBindingsUpdate)
  }

  private fun onBindingsUpdate(oldValue: List<Binding>, newValue: List<Binding>) {
    updateHighlighters(oldValue, newValue)
    updateItemBind()
    selectedItems = IRange.EMPTY_RANGE
    editorSelectionRange = IRange.EMPTY_RANGE
    fireStateChanged()
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

  // Do not use default selection
  private fun editorSelectionUpdated() {
    if (myIgnoreSelectionEvents) return
    myIgnoreSelectionEvents = true
    editor.selectionModel.removeSelection()
    myIgnoreSelectionEvents = false
  }

  override fun dispose() {
    diffModel.removeBindingsListener(this::onBindingsUpdate)
  }

  var editorHoveredLine: Int = -1
    set(value) {
      val newLine = if (value < 0) -1 else myLineRange.inside(value)
      if (field != newLine) {
        val x = myHoveredHighlighter
        if (x != null) {
          editor.markupModel.removeHighlighter(x)
        }
        myHoveredHighlighter = null
        if (newLine >= 0 && newLine !in editorSelectionRange) {
          val attributes = myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!
          attributes.backgroundColor = Settings.DIFF_HOVERED_COLOR
          myHoveredHighlighter = editor.markupModel.addLineHighlighter(
              newLine,
              HighlighterLayer.SELECTION,
              myEditorHoveredAttributes
          )
        }
        field = newLine
        fireStateChanged()
      }
    }

  var editorSelectionRange: IRange = IRange.EMPTY_RANGE
    set(value) {
      val newValue = value.intersect(myLineRange)
      if (field != newValue) {
        val removal = IRangeUnion()
        val addition = IRangeUnion()
        removal.union(field)
        removal.exclude(newValue)
        addition.union(newValue)
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
        field = newValue
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

  init {
    myEditorHoveredAttributes.copyFrom(myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!)
    myEditorHoveredAttributes.backgroundColor = Settings.DIFF_HOVERED_COLOR
    myEditorSelectionAttributes.copyFrom(myDefaultScheme.getAttributes(DefaultLanguageHighlighterColors.COMMA)!!)
    myEditorSelectionAttributes.backgroundColor = Settings.DIFF_SELECTED_COLOR
    editor.selectionModel.addSelectionListener(object : SelectionListener {
      override fun selectionChanged(e: SelectionEvent) {
        editorSelectionUpdated()
      }
    })
    editor.addEditorMouseMotionListener(myEditorDragListener)
    editor.addEditorMouseListener(myEditorDragListener)
    onBindingsUpdate(listOf(), diffModel.bindings)
  }

  fun selectHeightRange(heightRange: IRange) {
    selectedItems = toItemRange(heightRange)
  }

  fun bindSelected() {
    if (selectedItems.empty || editorSelectionRange.empty) return
    diffModel.bind(selectedItems, editorSelectionRange)
  }

  fun unbindSelected() {
    if (selectedItems.empty) return
    diffModel.unbind(selectedItems)
  }

  fun undo() {
    diffModel.undo()
  }

  fun redo() {
    diffModel.redo()
  }

  fun resetChanges() {
    diffModel.resetChanges()
  }

  val bindings get() = diffModel.bindings

  val isUndoAvailable get() = diffModel.isUndoAvailable

  val isRedoAvailable get() = diffModel.isRedoAvailable

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
    for (binding in diffModel.bindings) {
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

  private fun Editor.createHighlighter(line: IRange): List<RangeHighlighter> {
    return DiffDrawUtil.createHighlighter(this, line.start, line.end + 1, DiffSimulator, false)
  }
}