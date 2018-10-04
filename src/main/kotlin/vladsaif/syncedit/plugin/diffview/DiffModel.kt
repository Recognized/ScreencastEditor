package vladsaif.syncedit.plugin.diffview

import com.intellij.openapi.Disposable
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.lang.script.psi.BlockVisitor
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import java.util.*
import kotlin.collections.HashMap

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

  private val myListeners = mutableSetOf<(List<MergedLineMapping>, List<MergedLineMapping>) -> Unit>()
  private val myUndoStack = ArrayDeque<Map<Int, IRange>>(1)
  private val myRedoStack = ArrayDeque<Map<Int, IRange>>(1)
  private val myShiftedLines: IntArray
  private val myBackwardLines: IntArray

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

  private var myMapping: Map<Int, IRange> = origin.bindings.mapValues { (_, v) -> v.toLineRange() }.toMutableMap()
    set(value) {
      myChangesWereMade = value != myInitialMapping
      field = value
      val newBindings = createMergedLineMappings(value, this@DiffModel::scriptLinesToVisibleLines)
      mergedLineMappings = newBindings
    }
  var mergedLineMappings: List<MergedLineMapping> = createMergedLineMappings(myMapping, this@DiffModel::scriptLinesToVisibleLines)
    private set(newValue) {
      if (field != newValue) {
        val oldValue = field
        field = newValue
        fireStateChanged(oldValue, newValue)
      }
    }
  private val myInitialMapping = HashMap(myMapping)
  private var myChangesWereMade = false

  override fun dispose() {
    origin.applyBindings(myMapping)
  }

  fun addBindingsListener(listener: (List<MergedLineMapping>, List<MergedLineMapping>) -> Unit) {
    myListeners.add(listener)
  }

  fun removeBindingsListener(listener: (List<MergedLineMapping>, List<MergedLineMapping>) -> Unit) {
    myListeners.remove(listener)
  }

  private fun fireStateChanged(oldValue: List<MergedLineMapping>, newValue: List<MergedLineMapping>) {
    for (x in myListeners) {
      x(oldValue, newValue)
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
    myUndoStack.push(myMapping)
    myMapping = myInitialMapping
  }

  val isResetAvailable get() = myChangesWereMade

  val isUndoAvailable get() = !myUndoStack.isEmpty()

  val isRedoAvailable get() = !myRedoStack.isEmpty()

  fun undo() {
    if (!myUndoStack.isEmpty()) {
      myRedoStack.push(myMapping)
      myMapping = myUndoStack.pop()
    }
  }

  fun redo() {
    if (!myRedoStack.isEmpty()) {
      myUndoStack.push(myMapping)
      myMapping = myRedoStack.pop()
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
    val convertedRange by lazy(LazyThreadSafetyMode.NONE) { visibleLinesToScriptLines(editorLines) }
    val oldMapping = myMapping
    val newMapping = myMapping.toMutableMap()

    for (item in itemRange.toIntRange()) {
      if (isBind) newMapping[item] = convertedRange
      else newMapping.remove(item)
    }

    if (oldMapping != newMapping) {
      if (myUndoStack.size == UNDO_STACK_LIMIT) {
        myUndoStack.removeLast()
      }
      myUndoStack.push(oldMapping)
      myMapping = newMapping
    }
  }

  companion object {
    private const val UNDO_STACK_LIMIT = 16
  }
}