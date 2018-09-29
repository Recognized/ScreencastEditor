package vladsaif.syncedit.plugin

import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.psi.PsiDocumentManager

class TranscriptModelUndoableAction(
    private val model: ScreencastFile,
    private val currentData: TranscriptData,
    private val newData: TranscriptData
) : UndoableAction {

  init {
    model.transcriptPsi
        ?.let { PsiDocumentManager.getInstance(model.project).getDocument(it) }
        ?.let { addAffectedDocuments(DocumentReferenceManager.getInstance().create(it)) }
    model.scriptDocument
        ?.let { addAffectedDocuments(DocumentReferenceManager.getInstance().create(it)) }
  }

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