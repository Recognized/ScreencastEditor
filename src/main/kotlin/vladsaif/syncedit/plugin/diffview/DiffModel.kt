package vladsaif.syncedit.plugin.diffview

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.RangeMarker
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.lang.script.psi.BlockVisitor
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import java.util.*

class DiffModel(
    val origin: ScreencastFile
) : Disposable {

  init {
    if (origin.data == null) {
      throw IllegalStateException("Cannot create DiffModel from model without transcript data.")
    }
    if (origin.scriptPsi == null) {
      throw IllegalStateException("Cannot create DiffModel from model without script file.")
    }
    if (origin.scriptDocument == null) {
      throw AssertionError("Script document is accidentally null.")
    }
  }

  private val myTranscriptDataOnStart: TranscriptData = origin.data!!
  private val myUndoStack = ArrayDeque<TranscriptData>(1)
  private val myRedoStack = ArrayDeque<TranscriptData>(1)
  private val myShiftedLines: IntArray
  private val myBackwardLines: IntArray
  private var myChangesWereMade = false
  private val myListeners = mutableSetOf<(List<Binding>, List<Binding>) -> Unit>()

  private val myDataListener = object : ScreencastFile.Listener {
    override fun onTranscriptDataChanged() {
      myChangesWereMade = origin.data != myTranscriptDataOnStart
      bindings = createBindings(origin.bindings, this@DiffModel::scriptLinesToVisibleLines)
    }
  }

  init {
    origin.addTranscriptDataListener(myDataListener)
  }

  override fun dispose() {
    origin.removeTranscriptDataListener(myDataListener)
  }

  fun addBindingsListener(listener: (List<Binding>, List<Binding>) -> Unit) {
    myListeners.add(listener)
  }

  fun removeBindingsListener(listener: (List<Binding>, List<Binding>) -> Unit) {
    myListeners.remove(listener)
  }

  private fun fireStateChanged(oldValue: List<Binding>, newValue: List<Binding>) {
    for (x in myListeners) {
      x(oldValue, newValue)
    }
  }

  init {
    val document = origin.scriptDocument!!
    myShiftedLines = IntArray(document.lineCount) { it }
    myBackwardLines = IntArray(document.lineCount)
    BlockVisitor.visit(origin.scriptPsi!!) {
      if (TimeOffsetParser.isTimeOffset(it)) {
        myShiftedLines[document.getLineNumber(it.textOffset)] = -1
      }
    }
    var accumulator = 0
    for ((index, x) in myShiftedLines.withIndex()) {
      if (x == -1) accumulator++
      myBackwardLines[index] = x - accumulator
    }
    var pos = 0
    for (x in myShiftedLines) {
      if (x == -1) continue
      myShiftedLines[pos++] = x
    }
  }

  private fun visibleLinesToScriptLines(line: IRange) = IRange(myShiftedLines[line.start], myShiftedLines[line.end])

  private fun scriptLinesToVisibleLines(line: IRange) = IRange(myBackwardLines[line.start], myBackwardLines[line.end])

  fun resetChanges() {
    if (!myChangesWereMade) return
    myRedoStack.clear()
    if (myUndoStack.size == UNDO_STACK_LIMIT) {
      myUndoStack.removeLast()
    }
    myUndoStack.push(origin.data)
    origin.data = myTranscriptDataOnStart
  }

  val isResetAvailable get() = myChangesWereMade

  val isUndoAvailable get() = !myUndoStack.isEmpty()

  val isRedoAvailable get() = !myRedoStack.isEmpty()

  fun undo() {
    if (!myUndoStack.isEmpty()) {
      myRedoStack.push(origin.data)
      origin.data = myUndoStack.pop()
    }
  }

  fun redo() {
    if (!myRedoStack.isEmpty()) {
      myUndoStack.push(origin.data)
      origin.data = myRedoStack.pop()
    }
  }

  fun unbind(itemRange: IRange) {
    bindUnbind(false, itemRange = itemRange, editorLines = IRange.EMPTY_RANGE)
  }

  fun bind(itemRange: IRange, editorLines: IRange) {
    bindUnbind(true, itemRange = itemRange, editorLines = editorLines)
  }

  private fun bindUnbind(isBind: Boolean, itemRange: IRange, editorLines: IRange) {
    myRedoStack.clear()
    val convertedRange = visibleLinesToScriptLines(editorLines)
    if (myUndoStack.size == UNDO_STACK_LIMIT) {
      myUndoStack.removeLast()
    }
    myUndoStack.push(origin.data)
    val scriptDoc = origin.scriptDocument!!
    val newMarker by lazy {
      scriptDoc.createRangeMarker(
          scriptDoc.getLineStartOffset(convertedRange.start).also { println(it) },
          scriptDoc.getLineEndOffset(convertedRange.end).also(::println)
      )
    }
    val replacements = mutableMapOf<Int, RangeMarker?>()
    for (index in itemRange.toIntRange()) {
      replacements[index] = if (isBind) newMarker else null
    }
    for ((key, value) in replacements) {
      if (value == null) {
        origin.bindings.remove(key)
      } else {
        origin.bindings[key] = value
      }
    }
  }

  var bindings: List<Binding> = createBindings(origin.bindings, this@DiffModel::scriptLinesToVisibleLines)
    set(newValue) {
      if (field != newValue) {
        val oldValue = field
        field = newValue
        fireStateChanged(oldValue, newValue)
      }
    }

  companion object {
    private const val UNDO_STACK_LIMIT = 16
  }
}