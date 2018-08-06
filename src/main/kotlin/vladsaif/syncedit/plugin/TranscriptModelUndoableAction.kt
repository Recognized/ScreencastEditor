package vladsaif.syncedit.plugin

import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.UndoableAction

class TranscriptModelUndoableAction(
    private val model: MultimediaModel,
    private val currentData: TranscriptData,
    private val newData: TranscriptData
) : UndoableAction {

  private val myAffectedDocuments = mutableSetOf<DocumentReference>()

  override fun redo() {
    model.data = newData
  }

  override fun undo() {
    model.data = currentData
  }

  fun addAffectedDocuments(ref: DocumentReference) {
    myAffectedDocuments.add(ref)
  }

  override fun isGlobal() = false

  override fun getAffectedDocuments() = myAffectedDocuments.toTypedArray()
}