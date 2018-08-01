package vladsaif.syncedit.plugin

import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.UndoableAction

class TranscriptModelUndoableAction(
        private val model: TranscriptModel,
        private val currentData: TranscriptData,
        private val newData: TranscriptData
) : UndoableAction {

    private val affectedDocuments = mutableSetOf<DocumentReference>()

    override fun redo() {
        println("redo performed")
        model.data = newData
    }

    override fun undo() {
        println("undo performed")
        model.data = currentData
    }

    fun addAffectedDocuments(ref: DocumentReference) {
        affectedDocuments.add(ref)
    }

    override fun isGlobal() = false

    override fun getAffectedDocuments() = affectedDocuments.toTypedArray()
}